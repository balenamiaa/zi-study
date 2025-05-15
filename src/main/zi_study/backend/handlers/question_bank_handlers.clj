(ns zi-study.backend.handlers.question-bank-handlers
  (:require [ring.util.response :as resp]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [zi-study.backend.db :refer [db-pool]]
            [honey.sql :as h]
            [honey.sql.helpers :as hh]))

;; --- Helper Functions ---

(defn- unauthorized [message]
  (-> (resp/response {:error (or message "Unauthorized")})
      (resp/status 401)))

(defn- bad-request [message data]
  (-> (resp/response (merge {:error (or message "Bad Request")} data))
      (resp/status 400)))

(defn- not-found [message]
  (-> (resp/response {:error (or message "Not Found")})
      (resp/status 404)))

(defn- server-error [message e]
  (println "Server Error:" message)
  (println (ex-message e))
  (println (ex-data e))
  (.printStackTrace e)
  (-> (resp/response {:error (or message "Internal Server Error")})
      (resp/status 500)))

(defn- parse-query-param [params key type]
  (when-let [v (get params key)]
    (try
      (case type
        :int (Integer/parseInt v)
        :bool (Boolean/parseBoolean v)
        :csv (->> (str/split v #",") (map str/trim) (remove str/blank?) vec)
        v)
      (catch NumberFormatException _ nil)
      (catch Exception _ nil))))

(defn- get-user-id [request]
  (get-in request [:identity :id]))

(def ^:private jdbc-opts {:builder-fn rs/as-unqualified-kebab-maps})

(defn- execute-one! [sqlmap]
  (jdbc/execute-one! @db-pool (h/format sqlmap) jdbc-opts))

(defn- execute! [sqlmap]
  (jdbc/execute! @db-pool (h/format sqlmap) jdbc-opts))

(defn- query [sqlvec]
  (sql/query @db-pool sqlvec jdbc-opts))

(defn- parse-edn-field [data field-key]
  (if-let [edn-str (get data field-key)]
    (try
      (assoc data field-key (edn/read-string edn-str))
      (catch Exception _
        (assoc data field-key {:error "Failed to parse EDN data"})))
    data))

;; --- Handlers Implementation ---

(defn list-tags-handler [_request]
  (try
    (let [tags (map :tag-name (query ["SELECT DISTINCT tag_name FROM tags ORDER BY tag_name"]))]
      (resp/response {:tags tags}))
    (catch Exception e
      (server-error "Failed to retrieve tags" e))))

(defn list-sets-handler [request]
  (if-let [user-id (get-user-id request)]
    (try
      (let [params (:query-params request)
            page (max 1 (or (parse-query-param params "page" :int) 1))
            limit (max 1 (or (parse-query-param params "limit" :int) 10))
            offset (* (dec page) limit)
            sort-by (keyword (get params "sort_by" "created_at"))
            sort-order (keyword (get params "sort_order" "desc"))
            filter-tags (parse-query-param params "tags" :csv)
            search-term (get params "search")
            search-where (when (not (str/blank? search-term))
                           [:or
                            [:like :qs.title (str "%" search-term "%")]
                            [:like :qs.description (str "%" search-term "%")]
                            [:like :t.tag-name (str "%" search-term "%")]])

            tag-having (when (seq filter-tags)
                         [:>= [:count [:distinct :t.tag-name]] (count filter-tags)])

            tag-where (when (seq filter-tags)
                        [:in :t.tag-name filter-tags])

            query-map (-> (hh/select [:qs.set_id :set_id]
                                     [:qs.title :title]
                                     [:qs.description :description]
                                     [:qs.created_at :created_at]
                                     [[:raw "COUNT(DISTINCT q.question_id)"] :total_questions]
                                     [[:coalesce [:count [:distinct [:case [:<> :ua.answer_id nil] :q.question_id]]] 0] :answered_count]
                                     [[:coalesce [:count [:distinct [:case [:= :ua.is_correct 1] :q.question_id]]] 0] :correct_count])
                          (hh/from [:question_sets :qs])
                          (hh/left-join [:questions :q] [:= :qs.set_id :q.set_id])
                          (hh/left-join [:user_answers :ua] [:and [:= :q.question_id :ua.question_id] [:= :ua.user_id user-id]])
                          (hh/left-join [:question_set_tags :qst] [:= :qs.set_id :qst.set_id])
                          (hh/left-join [:tags :t] [:= :qst.tag_id :t.tag_id])
                          (cond-> search-where (hh/where search-where))
                          (cond-> tag-where (hh/where tag-where))
                          (hh/group-by :qs.set_id :qs.title :qs.description :qs.created_at)
                          (cond-> tag-having (hh/having tag-having))
                          (hh/order-by [sort-by sort-order])
                          (hh/limit limit)
                          (hh/offset offset))

            count-query-map (if (seq filter-tags)
                              (-> (hh/select [[:count :*] :total])
                                  (hh/from [(-> (hh/select :qs.set_id)
                                                (hh/from [:question_sets :qs])
                                                (hh/left-join [:question_set_tags :qst] [:= :qs.set_id :qst.set_id])
                                                (hh/left-join [:tags :t] [:= :qst.tag_id :t.tag_id])
                                                (cond-> search-where (hh/where search-where))
                                                (cond-> tag-where (hh/where tag-where))
                                                (hh/group-by :qs.set_id)
                                                (cond-> tag-having (hh/having tag-having)))
                                            :sub]))
                              (if (not (str/blank? search-term))
                                (-> (hh/select [[:count :*] :total])
                                    (hh/from [(-> (hh/select :qs.set_id)
                                                  (hh/from [:question_sets :qs])
                                                  (hh/left-join [:question_set_tags :qst] [:= :qs.set_id :qst.set_id])
                                                  (hh/left-join [:tags :t] [:= :qst.tag_id :t.tag_id])
                                                  (hh/where search-where)
                                                  (hh/group-by :qs.set_id))
                                              :sub]))
                                (-> (hh/select [[:count [:distinct :qs.set_id]] :total])
                                    (hh/from [:question_sets :qs]))))

            total-count-result (execute-one! count-query-map)
            total-items (:total total-count-result 0)
            total-pages (if (pos? total-items) (int (Math/ceil (/ (double total-items) limit))) 0)
            sets-raw (execute! query-map)

            sets-with-tags (mapv (fn [s]
                                   (let [set-id (:set-id s)
                                         tags-query ["SELECT t.tag_name FROM tags t JOIN question_set_tags qst ON t.tag_id = qst.tag_id WHERE qst.set_id = ? ORDER BY t.tag_name" set-id]
                                         tags (mapv :tag-name (query tags-query))]
                                     (assoc s :tags tags)))
                                 sets-raw)

            sets (mapv (fn [s]
                         (let [total (:total-questions s 0)
                               answered (:answered-count s 0)
                               correct (:correct-count s 0)]
                           (assoc s :progress {:total total
                                               :answered answered
                                               :correct correct
                                               :answered_percent (if (pos? total) (double (/ answered total)) 0.0)
                                               :correct_percent (if (pos? answered) (double (/ correct answered)) 0.0)})))
                       sets-with-tags)]

        (resp/response {:sets sets
                        :pagination {:page page
                                     :limit limit
                                     :total_items total-items
                                     :total_pages total-pages}}))
      (catch Exception e
        (server-error "Failed to retrieve question sets" e)))
    (unauthorized "Authentication required to list sets.")))


(defn get-set-details-handler [request]
  (if-let [_user-id (get-user-id request)]
    (try
      (let [set-id (parse-query-param (:path-params request) :set-id :int)]
        (if set-id
          (let [set-details (jdbc/execute-one! @db-pool
                                               ["SELECT * FROM question_sets WHERE set_id = ?" set-id]
                                               jdbc-opts)
                tags (or (seq (map :tag-name
                                   (query ["SELECT t.tag_name FROM tags t JOIN question_set_tags qst ON t.tag_id = qst.tag_id WHERE qst.set_id = ?" set-id]))) [])]
            (if set-details
              (resp/response (assoc set-details :tags tags))
              (not-found "Question set not found.")))
          (bad-request "Invalid or missing Set ID in path." nil)))
      (catch Exception e
        (server-error (str "Failed to retrieve details for set ID: " (get-in request [:path-params :set-id])) e)))
    (unauthorized "Authentication required for set details.")))

(defn get-set-questions-handler [request]
  (if-let [user-id (get-user-id request)]
    (try
      (let [set-id (parse-query-param (:path-params request) :set-id :int)
            params (:query-params request)
            filter-difficulty (parse-query-param params "difficulty" :int)
            filter-answered (parse-query-param params "answered" :bool)
            filter-correct (parse-query-param params "correct" :bool)
            filter-bookmarked (parse-query-param params "bookmarked" :bool)
            search-term (get params "search")]

        (if set-id
          (let [base-query {:select [:q.*
                                     [:ua.answer_data :user_answer_data]
                                     [:ua.is_correct :user_is_correct]
                                     [:ua.submitted_at :user_submitted_at]
                                     [:ub.bookmarked_at :user_bookmarked_at]]
                            :from [[:questions :q]]
                            :left-join [[:user_answers :ua] [:and [:= :q.question_id :ua.question_id] [:= :ua.user_id user-id]]
                                        [:user_bookmarks :ub] [:and [:= :q.question_id :ub.question_id] [:= :ub.user_id user-id]]]
                            :where [:= :q.set_id set-id]}

                filtered-query (cond-> base-query
                                 (some? filter-difficulty) (hh/where [:= :q.difficulty filter-difficulty])
                                 (some? filter-answered) (hh/where (if filter-answered [:is-not :ua.answer_id nil] [:is :ua.answer_id nil]))
                                 (some? filter-correct) (hh/where [:= :ua.is_correct (if filter-correct 1 0)])
                                 (true? filter-bookmarked) (hh/where [:is-not :ub.user_id nil])
                                 (not (str/blank? search-term)) (hh/where [:like :q.question_data (str "%" search-term "%")]))

                final-query (-> filtered-query
                                (hh/order-by :q.order_in_set :q.question_id)) ; Ensure consistent order

                questions-raw (execute! final-query)
                _ (prn questions-raw)
                questions (mapv (fn [q]
                                  (let [q (-> q
                                              (parse-edn-field :question-data)
                                              (#(if (:user-answer-data %) (parse-edn-field % :user-answer-data) %)))]
                                    (-> q
                                        (assoc :bookmarked (some? (:user-bookmarked-at q)))
                                        (assoc :user-answer (when (:user-answer-data q)
                                                              {:answer-data (:user-answer-data q)
                                                               :is-correct (:user-is-correct q)
                                                               :submitted-at (:user-submitted-at q)}))
                                        (dissoc :user-answer-data :user-is-correct :user-submitted-at :user-bookmarked-at))))
                                questions-raw)]

            (resp/response {:questions questions
                            :filters {:difficulty filter-difficulty
                                      :answered filter-answered
                                      :correct filter-correct
                                      :bookmarked filter-bookmarked
                                      :search search-term}}))
          (bad-request "Invalid or missing Set ID in path." nil)))
      (catch Exception e
        (server-error (str "Failed to retrieve questions for set ID: " (get-in request [:path-params :set-id])) e)))
    (unauthorized "Authentication required to get questions.")))

(defn- check-answer [question-type question-data-str user-answer-data]
  (try
    (let [q-data (edn/read-string question-data-str)
          user-answer (:answer user-answer-data)]
      (case (keyword question-type)
        :written nil
        :true-false (let [expected-correct (:is_correct_true q-data)] (= expected-correct user-answer))
        :mcq-single (= (:correct_index q-data) user-answer)
        :mcq-multi (= (set (:correct_indices q-data)) (set user-answer))
        :cloze (let [correct-answers (:answers q-data)]
                 (and (= (count correct-answers) (count user-answer))
                      (every? true? (map = (map str/trim correct-answers) (map str/trim user-answer)))))
        :emq (let [correct-matches-raw (:matches q-data)
                   ;; Normalize correct matches from either map or vector format
                   correct-matches (set (cond
                                          (map? correct-matches-raw) (map vec (seq correct-matches-raw))
                                          :else (map vec correct-matches-raw)))
                   ;; Normalize user answer from either map or vector format
                   user-matches (set (cond
                                       (map? user-answer) (map vec (seq user-answer))
                                       :else (map vec user-answer)))]
               (= correct-matches user-matches))
        false))
    (catch Exception e
      (println "Error checking answer:" (ex-message e) "QType:" question-type "QData:" question-data-str "User:" user-answer-data)
      false)))

(defn submit-answer-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [question-id (parse-query-param (:path-params request) :question-id :int)
          raw-answer-data (:body-params request)]
      (cond
        (not question-id)
        (bad-request "Invalid or missing Question ID in path." nil)

        (not raw-answer-data)
        (bad-request "Missing answer data in request body." nil)

        :else
        (jdbc/with-transaction [tx @db-pool {:builder-fn rs/as-unqualified-kebab-maps}]
          (try
            (let [question (first (sql/find-by-keys tx :questions {:question_id question-id}))]
              (if-not question
                (not-found "Question not found.")
                (let [q-type (:questions/question_type question)
                      q-data-str (:questions/question_data question)
                      is-correct-bool (check-answer q-type q-data-str raw-answer-data)
                      is-correct-int (cond
                                       (nil? is-correct-bool) nil
                                       is-correct-bool 1
                                       :else 0)
                      answer-data-str (pr-str raw-answer-data)

                      ;; Use parameterized query that properly handles NULL values
                      _ (if (nil? is-correct-int)
                          ;; For written questions (is_correct is NULL)
                          (jdbc/execute! tx [(str "INSERT OR REPLACE INTO user_answers "
                                                  "(user_id, question_id, answer_data, is_correct, submitted_at) "
                                                  "VALUES (?, ?, ?, NULL, CURRENT_TIMESTAMP)")
                                             user-id question-id answer-data-str]
                                         jdbc-opts)
                          ;; For other question types with automatic checking
                          (jdbc/execute! tx [(str "INSERT OR REPLACE INTO user_answers "
                                                  "(user_id, question_id, answer_data, is_correct, submitted_at) "
                                                  "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)")
                                             user-id question-id answer-data-str is-correct-int]
                                         jdbc-opts))]

                  (resp/response {:correct (when (some? is-correct-int) (= 1 is-correct-int))
                                  :correct_answer (:question-data (parse-edn-field question :question-data))}))))
            (catch Exception e
              (server-error (str "Failed to submit answer for question ID: " question-id) e))))))
    (unauthorized "Authentication required to submit answers.")))


(defn self-evaluate-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [question-id (parse-query-param (:path-params request) :question-id :int)
          body-params (:body-params request)
          is-correct (when (contains? body-params :is_correct) (:is_correct body-params))]
      (cond
        (not question-id)
        (bad-request "Invalid or missing Question ID." nil)

        (not (contains? body-params :is_correct))
        (bad-request "Missing 'is_correct' in request body." {:body body-params})

        (not (boolean? is-correct))
        (bad-request "Field 'is_correct' must be a boolean." {:body body-params})

        :else
        (jdbc/with-transaction [tx @db-pool {:builder-fn rs/as-unqualified-kebab-maps}]
          (try
            (let [question (sql/get-by-id tx :questions question-id :question_id {:columns [:question_type]})
                  user-answer (sql/find-by-keys tx :user_answers {:user_id user-id :question_id question-id} {:columns [:answer_id]})]

              (cond
                (not question)
                (not-found "Question not found.")
                (not= (:questions/question_type question) "written")
                (bad-request "Self-evaluation is only allowed for 'written' questions." {:question-type (:questions/question_type question)})

                (not user-answer)
                (not-found "No answer submitted yet for this question.")

                :else
                (let [rows-affected (:next.jdbc/update-count
                                     (sql/update! tx :user_answers
                                                  {:is_correct (if is-correct 1 0)}
                                                  {:user_id user-id :question_id question-id}))]
                  (if (= 1 rows-affected)
                    (resp/response {:success true})
                    (server-error (str "Failed to update self-evaluation status for answer: user " user-id ", question " question-id) (ex-info "0 rows affected" {}))))))
            (catch Exception e
              (server-error (str "Failed self-evaluation for question ID: " question-id) e))))))
    (unauthorized "Authentication required for self-evaluation.")))

(defn toggle-bookmark-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [question-id (parse-query-param (:path-params request) :question-id :int)
          body-params (:body-params request)
          new-bookmarked (:bookmarked body-params)]
      (if (and question-id (some? new-bookmarked))
        (jdbc/with-transaction [tx @db-pool {:builder-fn rs/as-unqualified-kebab-maps}]
          (try
            (if (not new-bookmarked)
              (do
                (sql/delete! tx :user_bookmarks {:user_id user-id :question_id question-id})
                (resp/response {:bookmarked false}))
              (let [existing (sql/find-by-keys tx :user_bookmarks
                                               {:user_id user-id :question_id question-id}
                                               {:columns [:user_id]})]
                (if (seq existing)
                  ;; If exists, do nothing or update timestamp if needed
                  (resp/response {:bookmarked true})
                  ;; If not exists, insert new bookmark
                  (do
                    (sql/insert! tx :user_bookmarks
                                 {:user_id user-id
                                  :question_id question-id
                                  :bookmarked_at [:raw "CURRENT_TIMESTAMP"]})
                    (resp/response {:bookmarked true})))))
            (catch Exception e
              (server-error (str "Failed to toggle bookmark for question ID: " question-id) e))))
        (bad-request "Invalid or missing parameters." nil)))
    (unauthorized "Authentication required for bookmarking.")))

(defn list-bookmarks-handler [request]
  (if-let [user-id (get-user-id request)]
    (try
      (let [query-sql ["SELECT ub.question_id, ub.bookmarked_at, q.question_type, q.question_data, qs.set_id, qs.title AS set_title
                        FROM user_bookmarks ub
                        JOIN questions q ON ub.question_id = q.question_id
                        JOIN question_sets qs ON q.set_id = qs.set_id
                        WHERE ub.user_id = ?
                        ORDER BY qs.title, q.order_in_set, q.question_id" user-id]
            bookmarks-raw (query query-sql)

            bookmarks-grouped (->> bookmarks-raw
                                   (map #(parse-edn-field % :question-data))
                                   (group-by (juxt :set-id :set-title))
                                   (mapv (fn [[[set-id set-title] questions]]
                                           {:set-id set-id
                                            :set-title set-title
                                            :questions (mapv #(dissoc % :set-id :set-title) questions)}))
                                   (sort-by :set-title))] ; Sort sets by title
        (resp/response {:bookmarks bookmarks-grouped}))
      (catch Exception e
        (server-error "Failed to retrieve bookmarks" e)))
    (unauthorized "Authentication required to list bookmarks.")))

(defn delete-answer-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [question-id (parse-query-param (:path-params request) :question-id :int)]
      (if question-id
        (try
          (let [rows-affected (:next.jdbc/update-count
                               (sql/delete! @db-pool
                                            :user_answers
                                            {:user_id user-id
                                             :question_id question-id}))]
            (resp/response {:success true
                            :deleted_count rows-affected}))
          (catch Exception e
            (server-error (str "Failed to delete answer for question ID: " question-id) e)))
        (bad-request "Invalid or missing Question ID in path." nil)))
    (unauthorized "Authentication required to delete answers.")))

(defn delete-set-answers-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [set-id (parse-query-param (:path-params request) :set-id :int)]
      (if set-id
        (try
          ;; First verify the set exists
          (let [set-exists? (boolean (sql/find-by-keys @db-pool :question_sets {:set_id set-id} {:columns [:set_id]}))]
            (if set-exists?
              (let [result (jdbc/execute! @db-pool
                                          [(str "DELETE FROM user_answers "
                                                "WHERE user_id = ? AND question_id IN "
                                                "(SELECT question_id FROM questions WHERE set_id = ?)")
                                           user-id
                                           set-id])
                    rows-affected (if (map? result)
                                    (:next.jdbc/update-count result)
                                    (count result))]
                (resp/response {:success true
                                :deleted_count rows-affected}))
              (not-found "Question set not found.")))
          (catch Exception e
            (server-error (str "Failed to delete answers for set ID: " set-id) e)))
        (bad-request "Invalid or missing Set ID in path." nil)))
    (unauthorized "Authentication required to delete answers.")))