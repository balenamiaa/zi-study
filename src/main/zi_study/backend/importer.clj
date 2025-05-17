(ns zi-study.backend.importer
  (:require [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [zi-study.backend.db :refer [db-pool]]
            [malli.core :as m]
            [malli.error :as me]
            [zi-study.backend.shapes.questions :refer [ImportData]]
            [zi-study.backend.shapes.question-content :as qc-shapes]
            [zi-study.backend.fts :as fts]))

(defn- to-str-temp-id [temp-id]
  (if (keyword? temp-id)
    (name temp-id)
    (str temp-id)))

(defn- get-flexible
  "Attempts to get a value from a map using a kebab-case key first.
   If not found, tries with the snake_case version of the key."
  ([m k not-found]
   (let [kebab-k k
         snake-k (keyword (str/replace (name k) "-" "_"))]
     (or (get m kebab-k not-found)
         (get m snake-k not-found))))

  ([m k]
   (get-flexible m k nil)))

(defn- parse-input [input-str format]
  (try
    (case format
      :edn (edn/read-string input-str)
      :json (json/read-str input-str :key-fn keyword))
    (catch Exception e
      (throw (ex-info (str "Failed to parse input string in " (name format) " format.")
                      {:error :parsing-failed :format format :exception e})))))

(defn- validate-data [data schema]
  (when-not (m/validate schema data)
    (throw (ex-info "Import data validation failed."
                    {:error :validation-failed
                     :explanation (me/humanize (m/explain schema data))}))))

(defn- get-or-create-tag [tx tag-name]
  (let [clean-tag-name (-> tag-name str .trim)]
    ;; First try: Find using an exact match
    (if-let [existing-tag-id (:tag_id (sql/find-by-keys tx :tags {:tag_name clean-tag-name} {:columns [:tag_id]}))]
      existing-tag-id ; Return existing ID if found
      ;; Not found with exact match, try case-insensitive match with SQL LIKE or LOWER
      (if-let [existing-tag-id (:tag_id (jdbc/execute-one! tx
                                                           ["SELECT tag_id FROM tags WHERE LOWER(tag_name) = LOWER(?)"
                                                            clean-tag-name]
                                                           {:builder-fn rs/as-unqualified-lower-maps}))]
        existing-tag-id
        ;; If still not found, create it
        (try
          (sql/insert! tx :tags {:tag_name clean-tag-name})
          (:last_insert_rowid
           (jdbc/execute-one! tx ["SELECT last_insert_rowid() AS last_insert_rowid"]
                              {:builder-fn rs/as-unqualified-lower-maps}))
          (catch Exception e
            ;; If insert fails due to constraint violation, try to find it again one last time
            ;; (handles race condition where tag might have been created between our check and insert)
            (let [error-msg (ex-message e)]
              (if (and (instance? org.sqlite.SQLiteException e)
                       (.contains error-msg "UNIQUE constraint failed: tags.tag_name"))
                (if-let [existing-tag-id (:tag_id (jdbc/execute-one! tx
                                                                     ["SELECT tag_id FROM tags WHERE LOWER(tag_name) = LOWER(?)"
                                                                      clean-tag-name]
                                                                     {:builder-fn rs/as-unqualified-lower-maps}))]
                  existing-tag-id
                  (throw (ex-info (str "Failed to get or create tag: " clean-tag-name)
                                  {:error :tag-creation-failed, :tag-name clean-tag-name} e)))
                ;; If it's another type of error, rethrow
                (throw e)))))))))

(defn- process-mcq-data [question]
  (let [options (get-flexible question :options)
        correct-temp-id (get-flexible question :correct-option-temp-id) ; Single MCQ
        correct-temp-ids (set (get-flexible question :correct-option-temp-ids)) ; Multi MCQ
        temp-to-index (into {} (map-indexed (fn [idx opt] [(get-flexible opt :temp-id) idx]) options))]
    (cond-> {:text (get-flexible question :question-text)
             :options (mapv #(get-flexible % :text) options) ; Store only text
             :explanation (get-flexible question :explanation)} ; Optional explanation
      correct-temp-id (assoc :correct-index (get temp-to-index correct-temp-id)) ; Single
      (seq correct-temp-ids) (assoc :correct-indices (->> options ; Multi
                                                          (keep-indexed (fn [idx opt]
                                                                          (when (correct-temp-ids (get-flexible opt :temp-id))
                                                                            idx)))
                                                          (into []))))))

(defn- process-written-data [question]
  {:text (get-flexible question :question-text)
   :correct-answer (get-flexible question :correct-answer-text)
   :explanation (get-flexible question :explanation)})

(defn- process-true-false-data [question]
  {:text (get-flexible question :question-text)
   :is-correct-true (get-flexible question :is-correct-answer-true)
   :explanation (get-flexible question :explanation)})

(defn- process-cloze-data [question]
  {:cloze-text (get-flexible question :cloze-text) ; e.g., "Hello {{c1::World}}."
   :answers (get-flexible question :answers)       ; e.g., ["World"]
   :explanation (get-flexible question :explanation)})

(defn- process-emq-data [question]
  (let [premises (get-flexible question :premises)
        options (get-flexible question :options)
        matches (get-flexible question :matches)
        premise-temp-to-index (into {} (map-indexed (fn [idx p] [(to-str-temp-id (get-flexible p :temp-id)) idx]) premises))
        option-temp-to-index (into {} (map-indexed (fn [idx o] [(to-str-temp-id (get-flexible o :temp-id)) idx]) options))]
    {:instructions (get-flexible question :instructions)
     :premises (mapv #(get-flexible % :text) premises)
     :options (mapv #(get-flexible % :text) options)
     :matches (->> matches
                   (mapv (fn [[p-temp o-temp]]
                           [(get premise-temp-to-index (to-str-temp-id p-temp))
                            (get option-temp-to-index (to-str-temp-id o-temp))]))
                   (filterv (fn [[p-idx o-idx]] (and (some? p-idx) (some? o-idx))))) ; Ensure both IDs were found
     :explanation (get-flexible question :explanation)}))

(defn- process-question
  "Transforms the validated question map into the format needed for DB insertion (esp. question_data)."
  [question]
  (let [q-type (keyword (get-flexible question :type))
        processed-map (case q-type
                        :mcq-single (process-mcq-data question)
                        :mcq-multi (process-mcq-data question)
                        :written (process-written-data question)
                        :true-false (process-true-false-data question)
                        :cloze (process-cloze-data question)
                        :emq (process-emq-data question)
                        (throw (ex-info (str "Unknown question type during import processing: " q-type) {:question question})))
        schema (case q-type
                 :mcq-single qc-shapes/McqQuestionContent
                 :mcq-multi qc-shapes/McqQuestionContent
                 :written qc-shapes/WrittenQuestionContent
                 :true-false qc-shapes/TrueFalseQuestionContent
                 :cloze qc-shapes/ClozeQuestionContent
                 :emq qc-shapes/EmqQuestionContent
                 nil)] ; Should not happen if q-type is known

    (when-not (and schema (m/validate schema processed-map))
      (throw (ex-info (str "Invalid processed question data structure for type: " q-type)
                      {:type q-type
                       :data processed-map
                       :explanation (when schema (me/humanize (m/explain schema processed-map)))
                       :input-question question})))

    {:set_id nil ; Will be set later
     :question_type (name q-type) ; Stays as string "mcq-single" etc.
     :difficulty (get-flexible question :difficulty)
     :question_data (pr-str processed-map) ; Store processed data as EDN string with kebab-case keys
     :retention_aid (get-flexible question :retention_aid)}))

(defn import-question-set-data!
  "Imports question set data from an EDN or JSON string or file path.
      Validates the data structure, resolves temporary IDs, and inserts into the database.
      Performs the import within a single transaction; rolls back on any error.
      `input` can be a file path (string) or the raw data string.
      `format` must be :edn or :json."
  [input format]
  (jdbc/with-transaction [tx @db-pool] ; Use the connection pool from db.clj
    (try
      (let [input-str (if (.exists (io/file input)) (slurp input) input) ; Read file if path exists
            raw-data (parse-input input-str format)
            _ (validate-data raw-data ImportData)
            results (atom [])]

        (doseq [set-data raw-data]
          (println (str "Importing set: " (:title set-data)))
          ;; 1. Insert Question Set and get ID
          (let [set-insert-data (select-keys set-data [:title :description])
                _ (sql/insert! tx :question_sets set-insert-data) ; Insert first
                ;; Then get the last inserted ID for SQLite
                set-id (:last_insert_rowid (jdbc/execute-one! tx ["SELECT last_insert_rowid() AS last_insert_rowid"]
                                                              {:builder-fn rs/as-unqualified-lower-maps}))]
            (when-not set-id
              (throw (ex-info "Failed to retrieve set_id after insert." {:set-title (:title set-data)})))
            (println (str "  Created set with ID: " set-id))

            ;; 2. Handle Tags
            (let [tag-ids (mapv #(get-or-create-tag tx %) (get-flexible set-data :tags []))]
              (doseq [tag-id tag-ids]
                (sql/insert! tx :question_set_tags {:set_id set-id :tag_id tag-id}))
              (println (str "  Associated tags: " (get-flexible set-data :tags []))))

            ;; 3. Process and Insert Questions
            (doseq [[idx question] (map-indexed vector (get-flexible set-data :questions))]
              (try
                (let [processed-q (process-question question)
                      q-insert-data (assoc processed-q
                                           :set_id set-id
                                           :order_in_set idx) ; Use index as order
                      ;; Use lower-case keys for result map
                      {:keys [question_id]} (sql/insert! tx :questions q-insert-data {:builder-fn rs/as-unqualified-lower-maps})]
                  (println (str "    Inserted question " idx " (temp: " (get-flexible question :temp-id) ") -> ID: " question_id))

                  ;; Update FTS table for the newly inserted question
                  (fts/update-question-fts! tx
                                            question_id
                                            set-id
                                            (:question_type q-insert-data)
                                            (:question_data q-insert-data)
                                            (:retention_aid q-insert-data)))
                (catch Exception e
                  (throw (ex-info (str "Failed to process/insert question at index " idx " in set '" (:title set-data) "'")
                                  {:error :question-processing-failed
                                   :set-title (:title set-data)
                                   :question-index idx
                                   :question-temp-id (get-flexible question :temp-id)
                                   :original-exception e}
                                  e)))))
            (swap! results conj {:set_id set-id :title (:title set-data)})))

        {:success true :imported-sets @results})

      (catch Exception e
        ;; Log the error details
        (println "Error during question set import! Transaction rolled back.")
        (println (ex-message e))
        (println (ex-data e))
        ;; Rethrow or return error map
        (throw (ex-info "Question set import failed."
                        {:success false :error :import-failed :details (ex-data e)}
                        e))))))


; --- Example Usage (in REPL) ---
(comment
  (def edn-example-str
    [{:title "Neuroscience Basics Revised"
      :description "Fundamental concepts in neuroscience, using temp IDs."
      :tags ["Neuroscience" "Biology" "Basics"]
      :questions
      [{:temp-id "q1-neuro-myelin"
        :type :mcq-single
        :difficulty 3
        :question-text "What is the primary function of the myelin sheath?"
        :retention-aid "Think insulation on a wire."
        :options [{:temp-id "q1-opt1" :text "Transmit signals"}
                  {:temp-id "q1-opt2" :text "Increase signal speed"}
                  {:temp-id "q1-opt3" :text "Provide nutrients"}
                  {:temp-id "q1-opt4" :text "Remove waste"}]
        :correct-option-temp-id "q1-opt2"
        :explanation "Myelin acts as an electrical insulator..."}
       {:temp_id "q2-neuro-lobes"
        :type :written
        :difficulty 2
        :question_text "Name the four lobes of the cerebral cortex."
        :retention_aid "FPOT: Frontal, Parietal, Occipital, Temporal"
        :correct_answer_text "Frontal lobe, Parietal lobe, Temporal lobe, Occipital lobe."}
       {:temp_id "q3-neuro-tf"
        :type :true-false
        :difficulty 1
        :question_text "Neurons communicate using electrical signals only."
        :is_correct_answer_true false
        :explanation "Neurons use both electrical and chemical signals."}
       {:temp_id "q4-neuro-multi"
        :type :mcq-multi
        :difficulty 4
        :question_text "Which of the following are neurotransmitters?"
        :options [{:temp_id "q4-optA" :text "Dopamine"}
                  {:temp_id "q4-optB" :text "Myelin"}
                  {:temp_id "q4-optC" :text "Serotonin"}
                  {:temp_id "q4-optD" :text "Actin"}]
        :correct_option_temp_ids ["q4-optA" "q4-optC"]}
       {:temp_id "q5-neuro-cloze"
        :type :cloze
        :difficulty 2
        :cloze_text "The gap between two neurons is called the {{c1::Synaptic Cleft}}. Neurotransmitters cross this gap from the {{c2}} terminal."
        :answers ["Synaptic Cleft" "presynaptic"]}
       {:temp-id "q6-neuro-emq"
        :type :emq
        :difficulty 4
        :instructions "Match each brain structure (Premise) with its primary function (Option)."
        :premises [{:temp-id "p1" :text "Hippocampus"}
                   {:temp-id "p2" :text "Amygdala"}
                   {:temp_id "p3" :text "Cerebellum"}]
        :options [{:temp-id "oA" :text "Emotion processing (especially fear)"}
                  {:temp-id "oB" :text "Memory formation"}
                  {:temp-id "oC" :text "Motor control and coordination"}
                  {:temp-id "oD" :text "Sensory relay station"}]
        :matches [["p1" "oB"]
                  ["p2" "oA"]
                  ["p3" "oC"]]
        :explanation "Myelin helps speed up nerve impulses."}]}])

  (import-question-set-data! (pr-str edn-example-str) :edn)

  (import-question-set-data! "questionz/OBGYN_st4_13_05_2025.edn" :edn)

  (sql/query @zi-study.backend.db/db-pool ["DELETE FROM question_sets WHERE set_id = 12"]))