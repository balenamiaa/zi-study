(ns zi-study.backend.fts
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn extract-text-from-question-data
  "Extracts all searchable text from a question's data map and retention aid.
   Input: question-type (keyword), question-data (Clojure map), retention-aid (string or nil), user-answer-data (optional).
   Returns: string with all searchable text concatenated."
  ([question-type question-data retention-aid]
   (extract-text-from-question-data question-type question-data retention-aid nil))

  ([question-type question-data retention-aid user-answer-data]
   (let [q-data (if (string? question-data) (edn/read-string question-data) question-data)
         user-data (when user-answer-data
                     (if (string? user-answer-data)
                       (edn/read-string user-answer-data)
                       user-answer-data))]
     (str/join " "
               (filter some?
                       (conj
                        (case question-type
                          :mcq-single
                          (concat [(get q-data :question-text) (get q-data :explanation)]
                                  (map :text (get q-data :options)))

                          :mcq-multi
                          (concat [(get q-data :question-text) (get q-data :explanation)]
                                  (map :text (get q-data :options)))

                          :written
                          (concat
                           [(get q-data :question-text) (get q-data :correct-answer-text) (get q-data :explanation)]
                           (when user-data [(get-in user-data [:answer])]))

                          :true-false
                          [(get q-data :question-text) (get q-data :explanation)]

                          :cloze
                          (concat
                           [(get q-data :cloze-text) (get q-data :explanation)]
                           (get q-data :answers)
                           (when user-data (get-in user-data [:answer])))

                          :emq
                          (concat [(get q-data :instructions) (get q-data :explanation)]
                                  (map :text (get q-data :premises))
                                  (map :text (get q-data :options)))

                          (list (str q-data))) ; Convert the whole map to string if unknown structure
                        retention-aid))))))

(defn update-question-fts!
  "Updates the questions_fts table for a given question.
   If user-answer-data is provided, includes that in the searchable text."
  ([db-conn question-id set-id question-type question-data-str retention-aid]
   (update-question-fts! db-conn question-id set-id question-type question-data-str retention-aid nil))

  ([db-conn question-id set-id question-type question-data-str retention-aid user-answer-data]
   (try
     (let [searchable-text (extract-text-from-question-data
                            (keyword question-type)
                            question-data-str
                            retention-aid
                            user-answer-data)]
       (jdbc/execute-one! db-conn
                          ["INSERT OR REPLACE INTO questions_fts (question_id, set_id, searchable_text)
                            VALUES (?, ?, ?)" question-id set-id searchable-text]
                          {:builder-fn rs/as-unqualified-kebab-maps}))
     (catch Exception e
       (println (str "Error updating questions_fts for question_id " question-id ": " (ex-message e)))
       ;; Optionally re-throw or handle more gracefully
       ))))