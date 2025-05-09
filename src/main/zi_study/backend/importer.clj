(ns zi-study.backend.importer
  (:require [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [zi-study.backend.db :refer [db-pool]] ; Get the HikariDataSource atom
            [malli.core :as m]
            [malli.error :as me]))



(def NonBlankString [:string {:min 1}])
(def OptionalString [:maybe :string])
(def Difficulty [:int {:min 1 :max 5}])
(def QuestionType [:enum "written" "mcq-single" "mcq-multi" "emq" "cloze" "true-false"])
(def TempId [:or :string :keyword]) ; Allow strings or keywords for temp IDs

(def McqOption
  [:map
   [:temp_id TempId]
   [:text NonBlankString]])

(def McqSingleQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :mcq-single "mcq-single"]] ; Allow keyword or string
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:options [:vector {:min 1} McqOption]]
   [:correct_option_temp_id TempId]
   [:explanation {:optional true} OptionalString]]) ; Optional explanation field

(def McqMultiQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :mcq-multi "mcq-multi"]]
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:options [:vector {:min 1} McqOption]]
   [:correct_option_temp_ids [:vector {:min 1} TempId]]
   [:explanation {:optional true} OptionalString]])

(def WrittenQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :written "written"]]
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:correct_answer_text NonBlankString]
   [:explanation {:optional true} OptionalString]])

(def TrueFalseQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :true-false "true-false"]]
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:is_correct_answer_true :boolean]
   [:explanation {:optional true} OptionalString]])

(def ClozeQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :cloze "cloze"]]
   [:difficulty {:optional true} Difficulty]
   [:cloze_text NonBlankString] ; Contains {{c1::hint}} or {{c1}} placeholders
   [:retention_aid {:optional true} OptionalString]
   [:answers [:vector {:min 1} NonBlankString]] ; Answers in order of placeholders
   [:explanation {:optional true} OptionalString]])

(def EmqPremise
  [:map
   [:temp_id TempId]
   [:text NonBlankString]])

(def EmqOption
  [:map
   [:temp_id TempId]
   [:text NonBlankString]])

(def EmqMatch [:tuple TempId TempId]) ; [premise_temp_id, option_temp_id]

(def EmqQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :emq "emq"]]
   [:difficulty {:optional true} Difficulty]
   [:instructions {:optional true} NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:premises [:vector {:min 1} EmqPremise]]
   [:options [:vector {:min 1} EmqOption]]
   [:matches [:vector {:min 1} EmqMatch]]
   [:explanation {:optional true} OptionalString]])

(def Question
  [:multi {:dispatch :type}
   [:mcq-single McqSingleQuestion]
   ["mcq-single" McqSingleQuestion] ; Handle string type
   [:mcq-multi McqMultiQuestion]
   ["mcq-multi" McqMultiQuestion]
   [:written WrittenQuestion]
   ["written" WrittenQuestion]
   [:true-false TrueFalseQuestion]
   ["true-false" TrueFalseQuestion]
   [:cloze ClozeQuestion]
   ["cloze" ClozeQuestion]
   [:emq EmqQuestion]
   ["emq" EmqQuestion]])

(def QuestionSet
  [:map {:closed true}
   [:title NonBlankString]
   [:description {:optional true} OptionalString]
   [:tags {:optional true} [:vector NonBlankString]]
   [:questions [:vector {:min 1} Question]]])

(def ImportData [:vector {:min 1} QuestionSet])


;; --- Helper Functions ---

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
  (let [options (:options question)
        correct-temp-id (get question :correct_option_temp_id) ; Single MCQ
        correct-temp-ids (set (get question :correct_option_temp_ids)) ; Multi MCQ
        temp-to-index (into {} (map-indexed (fn [idx opt] [(get opt :temp_id) idx]) options))]
    (cond-> {:text (:question_text question)
             :options (mapv :text options) ; Store only text
             :explanation (:explanation question)} ; Optional explanation
      correct-temp-id (assoc :correct_index (get temp-to-index correct-temp-id)) ; Single
      correct-temp-ids (assoc :correct_indices (->> options ; Multi
                                                    (keep-indexed (fn [idx opt]
                                                                    (when (correct-temp-ids (get opt :temp_id))
                                                                      idx)))
                                                    (into []))))))

(defn- process-written-data [question]
  {:text (:question_text question)
   :correct_answer (:correct_answer_text question)
   :explanation (:explanation question)})

(defn- process-true-false-data [question]
  {:text (:question_text question)
   :is_correct_true (:is_correct_answer_true question)
   :explanation (:explanation question)})

(defn- process-cloze-data [question]
  {:cloze_text (:cloze_text question) ; e.g., "Hello {{c1::World}}."
   :answers (:answers question)       ; e.g., ["World"]
   :explanation (:explanation question)})

(defn- process-emq-data [question]
  (let [premises (:premises question)
        options (:options question)
        matches (:matches question)
        premise-temp-to-index (into {} (map-indexed (fn [idx p] [(get p :temp_id) idx]) premises))
        option-temp-to-index (into {} (map-indexed (fn [idx o] [(get o :temp_id) idx]) options))]
    {:instructions (:instructions question)
     :premises (mapv :text premises)
     :options (mapv :text options)
     :matches (->> matches
                   (mapv (fn [[p-temp o-temp]]
                           [(get premise-temp-to-index p-temp) (get option-temp-to-index o-temp)]))
                   (filterv (fn [[p-idx o-idx]] (and (some? p-idx) (some? o-idx))))) ; Ensure both IDs were found
     :explanation (:explanation question)}))

(defn- process-question
  "Transforms the validated question map into the format needed for DB insertion (esp. question_data)."
  [question]
  (let [q-type (keyword (:type question))
        processor (case q-type
                    :mcq-single process-mcq-data
                    :mcq-multi process-mcq-data
                    :written process-written-data
                    :true-false process-true-false-data
                    :cloze process-cloze-data
                    :emq process-emq-data)]
    {:set_id nil ; Will be set later
     :question_type (name q-type)
     :difficulty (:difficulty question)
     :question_data (pr-str (processor question)) ; Store processed data as EDN string
     :retention_aid (:retention_aid question)}))

;; --- Main Import Function ---

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
            (let [tag-ids (mapv #(get-or-create-tag tx %) (:tags set-data []))]
              (doseq [tag-id tag-ids]
                (sql/insert! tx :question_set_tags {:set_id set-id :tag_id tag-id}))
              (println (str "  Associated tags: " (:tags set-data []))))

            ;; 3. Process and Insert Questions
            (doseq [[idx question] (map-indexed vector (:questions set-data))]
              (try
                (let [processed-q (process-question question)
                      q-insert-data (assoc processed-q
                                           :set_id set-id
                                           :order_in_set idx) ; Use index as order
                      ;; Use lower-case keys for result map
                      {:keys [question_id]} (sql/insert! tx :questions q-insert-data {:builder-fn rs/as-unqualified-lower-maps})]
                  (println (str "    Inserted question " idx " (temp: " (:temp_id question) ") -> ID: " question_id)))
                (catch Exception e
                  (throw (ex-info (str "Failed to process/insert question at index " idx " in set '" (:title set-data) "'")
                                  {:error :question-processing-failed
                                   :set-title (:title set-data)
                                   :question-index idx
                                   :question-temp-id (:temp_id question)
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


;; --- Example Usage (in REPL) ---
(comment
  (require '[zi-study.backend.db :as db])
  (db/init-db!)

  (def edn-example-str
    [{:title "Neuroscience Basics Revised"
      :description "Fundamental concepts in neuroscience, using temp IDs."
      :tags ["Neuroscience" "Biology" "Basics"]
      :questions
      [{:temp_id "q1-neuro-myelin"
        :type :mcq-single
        :difficulty 3
        :question_text "What is the primary function of the myelin sheath?"
        :retention_aid "Think insulation on a wire."
        :options [{:temp_id "q1-opt1" :text "Transmit signals"}
                  {:temp_id "q1-opt2" :text "Increase signal speed"}
                  {:temp_id "q1-opt3" :text "Provide nutrients"}
                  {:temp_id "q1-opt4" :text "Remove waste"}]
        :correct_option_temp_id "q1-opt2"
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
       {:temp_id "q6-neuro-emq"
        :type :emq
        :difficulty 4
        :instructions "Match each brain structure (Premise) with its primary function (Option)."
        :premises [{:temp_id "p1" :text "Hippocampus"}
                   {:temp_id "p2" :text "Amygdala"}
                   {:temp_id "p3" :text "Cerebellum"}]
        :options [{:temp_id "oA" :text "Emotion processing (especially fear)"}
                  {:temp_id "oB" :text "Memory formation"}
                  {:temp_id "oC" :text "Motor control and coordination"}
                  {:temp_id "oD" :text "Sensory relay station"}]
        :matches [["p1" "oB"]
                  ["p2" "oA"]
                  ["p3" "oC"]]}]}])

  ;; Import from EDN string
  (import-question-set-data! (pr-str edn-example-str) :edn)
  ;; Assuming you have a file 'import_data.edn' or 'import_data.json'
  ;; (import-question-set-data! "path/to/your/import_data.edn" :edn)
  ;; (import-question-set-data! "path/to/your/import_data.json" :json)

  (import-question-set-data! "questionz/lower_gi_bleeding.edn" :edn)

  ;; Verify insertion (using next.jdbc directly for quick check)
  (jdbc/execute! @db/db-pool ["SELECT * FROM question_sets;"])
  (jdbc/execute! @db/db-pool ["SELECT * FROM tags;"])
  (jdbc/execute! @db/db-pool ["SELECT * FROM question_set_tags;"])
  (jdbc/execute! @db/db-pool ["SELECT question_id, set_id, question_type, difficulty, order_in_set, question_data FROM questions ORDER BY set_id, order_in_set;"])

  ;; Example: Read back and parse question_data for a specific question
  (let [q-data-str (:question_data (first (sql/find-by-keys @db/db-pool :questions {:question_id 1})))]
    (clojure.edn/read-string q-data-str))


  (db/close-db!) ; Clean up pool if needed
  )
 