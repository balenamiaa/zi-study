(ns zi-study.backend.handlers.active-learning-handlers
  (:require [ring.util.response :as resp]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [zi-study.backend.db :refer [db-pool]]
            [honey.sql :as h]
            [honey.sql.helpers :as hh]
            [zi-study.backend.fts :as fts]))

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
  (if e
    (do
      (println (ex-message e))
      (println (ex-data e))
      (.printStackTrace e))
    (println "No exception object provided to server-error function."))
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

(defn list-tags-handler
  "Returns a list of all available tags"
  [_request]
  (try
    (let [tags (map :tag-name (query ["SELECT DISTINCT tag_name FROM tags ORDER BY tag_name"]))]
      (resp/response {:tags tags}))
    (catch Exception e
      (server-error "Failed to retrieve tags" e))))

(defn list-sets-handler
  "Returns a paginated list of question sets with filtering options"
  [request]
  (if-let [user-id (get-user-id request)]
    (try
      (let [params (:query-params request)
            page (max 1 (or (parse-query-param params "page" :int) 1))
            limit (max 1 (or (parse-query-param params "limit" :int) 10))
            offset (* (dec page) limit)
            raw-sort-by (get params "sort_by" "updated_at")
            sort-by (cond
                      (= "updated_at" raw-sort-by) :qs.updated_at
                      (= "created_at" raw-sort-by) :qs.created_at
                      :else (keyword raw-sort-by))
            sort-order (keyword (get params "sort_order" "desc"))
            filter-tags (parse-query-param params "tags" :csv)
            search-term (get params "search")
            exclude-folder-id (parse-query-param params "exclude-sets-from-folder-id" :int)

            search-where (when (not (str/blank? search-term))
                           [:or
                            [:like :qs.title (str "%" search-term "%")]
                            [:like :qs.description (str "%" search-term "%")]
                            [:like :t.tag-name (str "%" search-term "%")]])

            tag-having (when (seq filter-tags)
                         [:>= [:count [:distinct :t.tag-name]] (count filter-tags)])

            tag-where (when (seq filter-tags)
                        [:in :t.tag-name filter-tags])

            ;; Subquery condition to exclude sets from a specific folder
            exclude-folder-subquery_condition (when exclude-folder-id
                                                [:not [:exists (-> (hh/select 1)
                                                                   (hh/from [:folder_question_sets :fqs_exclude])
                                                                   (hh/where [:and
                                                                              [:= :fqs_exclude.folder_id exclude-folder-id]
                                                                              [:= :fqs_exclude.set_id :qs.set_id]]))]])

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
                          (cond-> exclude-folder-subquery_condition (hh/where exclude-folder-subquery_condition))
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
                                                (cond-> exclude-folder-subquery_condition (hh/where exclude-folder-subquery_condition))
                                                (hh/group-by :qs.set_id)
                                                (cond-> tag-having (hh/having tag-having)))
                                            :sub]))
                              (if (not (str/blank? search-term))
                                (-> (hh/select [[:count :*] :total])
                                    (hh/from [(-> (hh/select :qs.set_id)
                                                  (hh/from [:question_sets :qs])
                                                  (hh/left-join [:question_set_tags :qst] [:= :qs.set_id :qst.set_id])
                                                  (hh/left-join [:tags :t] [:= :qst.tag_id :t.tag_id])
                                                  (cond-> search-where (hh/where search-where))
                                                  (cond-> exclude-folder-subquery_condition (hh/where exclude-folder-subquery_condition))
                                                  (hh/group-by :qs.set_id))
                                              :sub]))
                                (-> (hh/select [[:count [:distinct :qs.set_id]] :total])
                                    (hh/from [:question_sets :qs])
                                    ;; Apply exclusion directly if no other complex joins/groups are involved in this simplest count path
                                    (cond-> exclude-folder-subquery_condition (hh/where exclude-folder-subquery_condition)))))

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
                                               :answered-percent (if (pos? total) (double (/ answered total)) 0.0)
                                               :correct-percent (if (pos? answered) (double (/ correct answered)) 0.0)})))
                       sets-with-tags)]

        (resp/response {:sets sets
                        :pagination {:page page
                                     :limit limit
                                     :total_items total-items
                                     :total_pages total-pages}}))
      (catch Exception e
        (server-error "Failed to retrieve question sets" e)))
    (unauthorized "Authentication required to list sets.")))

(defn get-set-details-handler
  "Returns detailed information about a specific question set"
  [request]
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
                                (hh/order-by :q.order_in_set :q.question_id))

                questions-raw (execute! final-query)
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
        :true-false (let [expected-correct (:is-correct-true q-data)] (= expected-correct user-answer))
        :mcq-single (= (:correct-index q-data) user-answer)
        :mcq-multi (= (set (:correct-indices q-data)) (set user-answer))
        :cloze (let [correct-answers (:answers q-data)]
                 (and (= (count correct-answers) (count user-answer))
                      (every? true? (map = (map str/trim correct-answers) (map str/trim user-answer)))))
        :emq (let [correct-matches-raw (:matches q-data)
                   ; Normalize correct matches from either map or vector format
                   correct-matches (set (cond
                                          (map? correct-matches-raw) (map vec (seq correct-matches-raw))
                                          :else (map vec correct-matches-raw)))
                   ; Normalize user answer from either map or vector format
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
    (let [question-id (parse-query-param (:path-params request) :question-id :string)
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

                      _ (if (nil? is-correct-int)
                          (jdbc/execute! tx [(str "INSERT OR REPLACE INTO user_answers "
                                                  "(user_id, question_id, answer_data, is_correct, submitted_at) "
                                                  "VALUES (?, ?, ?, NULL, CURRENT_TIMESTAMP)")
                                             user-id question-id answer-data-str]
                                         jdbc-opts)
                          (jdbc/execute! tx [(str "INSERT OR REPLACE INTO user_answers "
                                                  "(user_id, question_id, answer_data, is_correct, submitted_at) "
                                                  "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)")
                                             user-id question-id answer-data-str is-correct-int]
                                         jdbc-opts))
                      _ (when (contains? #{"cloze" "written"} q-type)
                          (fts/update-question-fts! tx
                                                    question-id
                                                    (:questions/set_id question)
                                                    q-type
                                                    q-data-str
                                                    (:questions/retention_aid question)
                                                    answer-data-str))]

                  (resp/response {:correct (when (some? is-correct-int) (= 1 is-correct-int))
                                  :correct-answer (:question-data (parse-edn-field question :question-data))}))))
            (catch Exception e
              (server-error (str "Failed to submit answer for question ID: " question-id) e))))))
    (unauthorized "Authentication required to submit answers.")))

(defn self-evaluate-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [question-id (parse-query-param (:path-params request) :question-id :string)
          body-params (:body-params request)
          is-correct (when (contains? body-params :is-correct) (:is-correct body-params))]
      (cond
        (not question-id)
        (bad-request "Invalid or missing Question ID." nil)

        (not (contains? body-params :is-correct))
        (bad-request "Missing 'is-correct' in request body." {:body body-params})

        (not (boolean? is-correct))
        (bad-request "Field 'is-correct' must be a boolean." {:body body-params})

        :else
        (jdbc/with-transaction [tx @db-pool {:builder-fn rs/as-unqualified-kebab-maps}]
          (try
            (let [question (sql/get-by-id tx :questions question-id :question_id {:columns [:question_type :set_id :question_data :retention_aid]})
                  user-answer (sql/find-by-keys tx :user_answers {:user_id user-id :question_id question-id} {:columns [:answer_id :answer_data]})]

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
                                                  {:user_id user-id :question_id question-id}))

                      ;; After self-evaluation, update the FTS with the user's written answer for better search
                      _ (when (= "written" (:questions/question_type question))
                          (fts/update-question-fts! tx
                                                    question-id
                                                    (:questions/set_id question)
                                                    (:questions/question_type question)
                                                    (:questions/question_data question)
                                                    (:questions/retention_aid question)
                                                    (:answer_data user-answer)))]

                  (if (= 1 rows-affected)
                    (resp/response {:success true})
                    (server-error (str "Failed to update self-evaluation status for answer: user " user-id ", question " question-id) (ex-info "0 rows affected" {}))))))
            (catch Exception e
              (server-error (str "Failed self-evaluation for question ID: " question-id) e))))))
    (unauthorized "Authentication required for self-evaluation.")))

(defn toggle-bookmark-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [question-id (parse-query-param (:path-params request) :question-id :string)
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
                  (resp/response {:bookmarked true})
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
                                   (sort-by :set-title))]
        (resp/response {:bookmarks bookmarks-grouped}))
      (catch Exception e
        (server-error "Failed to retrieve bookmarks" e)))
    (unauthorized "Authentication required to list bookmarks.")))

(defn delete-answer-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [question-id (parse-query-param (:path-params request) :question-id :string)]
      (if question-id
        (try
          (let [rows-affected (:next.jdbc/update-count
                               (sql/delete! @db-pool
                                            :user_answers
                                            {:user_id user-id
                                             :question_id question-id}))]
            (resp/response {:success true
                            :deleted-count rows-affected}))
          (catch Exception e
            (server-error (str "Failed to delete answer for question ID: " question-id) e)))
        (bad-request "Invalid or missing Question ID in path." nil)))
    (unauthorized "Authentication required to delete answers.")))

(defn delete-set-answers-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [set-id (parse-query-param (:path-params request) :set-id :int)]
      (if set-id
        (try
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
                                :deleted-count rows-affected}))
              (not-found "Question set not found.")))
          (catch Exception e
            (server-error (str "Failed to delete answers for set ID: " set-id) e)))
        (bad-request "Invalid or missing Set ID in path." nil)))
    (unauthorized "Authentication required to delete answers.")))

(defn search-questions-handler [request]
  (if-let [user-id (get-user-id request)]
    (try
      (let [params (:query-params request)
            keywords (get params "keywords")
            page (max 1 (or (parse-query-param params "page" :int) 1))
            limit (max 1 (or (parse-query-param params "limit" :int) 15)) ;; Default limit for search results
            offset (* (dec page) limit)]

        (if (str/blank? keywords)
          (bad-request "Search keywords are required." nil)
          (let [fts-match-query (str keywords "*") ;; Use prefix matching for FTS

                ;; Get total count for pagination from FTS table
                count_query_map (-> (hh/select [[:count :*] :total])
                                    (hh/from [:questions_fts :q_fts])
                                    (hh/where [:raw "searchable_text MATCH " [:lift fts-match-query]]))
                total-items (:total (execute-one! count_query_map) 0)
                total-pages (if (pos? total-items) (int (Math/ceil (/ (double total-items) limit))) 0)

                questions (if (and (pos? total-items) (> total-pages (dec page)))
                            (let [questions_query_map (-> (hh/select :q.*
                                                                     [:qs.title :question_set_title] ; Ensure set title is selected
                                                                     [:ua.answer_data :user_answer_data]
                                                                     [:ua.is_correct :user_is_correct]
                                                                     [:ua.submitted_at :user_submitted_at]
                                                                     [:ub.bookmarked_at :user_bookmarked_at])
                                                          (hh/from [:questions_fts :q_fts])
                                                          (hh/join [:questions :q] [:= :q.question_id :q_fts.question_id]
                                                                   [:question_sets :qs] [:= :q.set_id :qs.set_id])
                                                          (hh/left-join [:user_answers :ua] [:and [:= :q.question_id :ua.question_id] [:= :ua.user_id user-id]]
                                                                        [:user_bookmarks :ub] [:and [:= :q.question_id :ub.question_id] [:= :ub.user_id user-id]])
                                                          (hh/where [:raw "searchable_text MATCH " [:lift fts-match-query]])
                                                          ;; Relying on FTS default ordering by relevance
                                                          (hh/limit limit)
                                                          (hh/offset offset))
                                  questions-raw (execute! questions_query_map)
                                  parsed-questions (mapv (fn [q]
                                                           (let [parsed-q (-> q
                                                                              (parse-edn-field :question-data)
                                                                              (#(if (:user-answer-data %) (parse-edn-field % :user-answer-data) %)))]
                                                             (-> parsed-q
                                                                 (assoc :bookmarked (some? (:user-bookmarked-at parsed-q)))
                                                                 (assoc :user-answer (when (:user-answer-data parsed-q)
                                                                                       {:answer-data (:user-answer-data parsed-q)
                                                                                        :is-correct (:user-is-correct parsed-q)
                                                                                        :submitted-at (:user-submitted-at parsed-q)}))
                                                                 (dissoc :user-answer-data :user-is-correct :user-submitted-at :user-bookmarked-at))))
                                                         questions-raw)]
                              parsed-questions)
                            [])]

            (resp/response {:questions questions
                            :pagination {:page page
                                         :limit limit
                                         :total_items total-items
                                         :total_pages total-pages}
                            :filters {:keywords keywords}}))))
      (catch Exception e
        (server-error (str "Failed to perform advanced search for keywords: " (get-in request [:query-params "keywords"])) e)))
    (unauthorized "Authentication required for advanced search.")))

(defn create-folder-handler
  "Creates a new folder for the authenticated user"
  [request]
  (if-let [user-id (get-user-id request)]
    (let [{:keys [name description is-public] :or {is-public false}} (:body-params request)
          is-public-bool (boolean is-public)]
      (if (str/blank? name)
        (bad-request "Folder name cannot be empty." nil)
        (try
          (let [new-folder-id (:folder-id (execute-one!
                                           (-> (hh/insert-into :folders)
                                               (hh/values [{:user_id user-id
                                                            :name name
                                                            :description description
                                                            :is_public is-public-bool
                                                            :created_at [:raw "CURRENT_TIMESTAMP"]
                                                            :updated_at [:raw "CURRENT_TIMESTAMP"]}])
                                               (hh/returning :folder_id))))]
            (if new-folder-id
              (let [complete-folder (execute-one!
                                     (-> (hh/select :f.folder_id :f.name :f.description :f.is_public
                                                    :f.created_at :f.updated_at
                                                    [[:raw "0"] :set_count])
                                         (hh/from [:folders :f])
                                         (hh/where [:= :f.folder_id new-folder-id])))]
                (resp/response complete-folder))
              (server-error "Failed to create folder, no ID returned." nil)))
          (catch Exception e
            (server-error (str "Failed to create folder: " (.getMessage e)) e)))))
    (unauthorized "Authentication required to create a folder.")))

(defn list-user-folders-handler
  "Lists all folders owned by the authenticated user"
  [request]
  (if-let [user-id (get-user-id request)]
    (try
      (let [folders (execute!
                     (-> (hh/select :f.folder_id :f.name :f.description :f.is_public :f.created_at :f.updated_at
                                    [[:count :fqs.set_id] :set_count])
                         (hh/from [:folders :f])
                         (hh/left-join [:folder_question_sets :fqs] [:= :f.folder_id :fqs.folder_id])
                         (hh/where [:= :f.user_id user-id])
                         (hh/group-by :f.folder_id :f.name :f.description :f.is_public :f.created_at :f.updated_at)
                         (hh/order-by [:f.updated_at :desc])))]
        (resp/response {:folders folders}))
      (catch Exception e
        (server-error "Failed to retrieve user folders." e)))
    (unauthorized "Authentication required to list your folders.")))

(defn list-public-folders-handler [request]
  (try
    (let [params (:query-params request)
          page (max 1 (or (parse-query-param params "page" :int) 1))
          limit (max 1 (or (parse-query-param params "limit" :int) 10))
          offset (* (dec page) limit)
          raw-sort-by (get params "sort_by" "f.updated_at") ; Default to f.updated_at and expect qualified
          sort-by (cond
                    (= "updated_at" raw-sort-by) :f.updated_at ; ensure qualified if ambiguous default was passed
                    (= "created_at" raw-sort-by) :f.created_at
                    ; Add other allowed sort fields here, qualified if necessary
                    :else (keyword raw-sort-by))
          sort-order (keyword (get params "sort_order" "desc"))
          search-term (get params "search")

          search-where (when-not (str/blank? search-term)
                         [:or
                          [:like :f.name (str "%" search-term "%")]
                          [:like :f.description (str "%" search-term "%")]
                          [:like :u.email (str "%" search-term "%")]])

          base-query (-> (hh/select :f.folder_id :f.name :f.description :f.created_at :f.updated_at
                                    :u.email :u.profile_picture_url
                                    [[:count :fqs.set_id] :set_count])
                         (hh/from [:folders :f])
                         (hh/join [:users :u] [:= :f.user_id :u.id])
                         (hh/left-join [:folder_question_sets :fqs] [:= :f.folder_id :fqs.folder_id])
                         (hh/where [:= :f.is_public true])
                         (cond-> search-where (hh/where search-where))
                         (hh/group-by :f.folder_id :f.name :f.description :f.created_at :f.updated_at :u.email :u.profile_picture_url))

          folders-query (-> base-query
                            (hh/order-by [sort-by sort-order])
                            (hh/limit limit)
                            (hh/offset offset))

          count-query (-> (hh/select [[:count :*] :total])
                          (hh/from [base-query :sub]))

          total-items (:total (execute-one! count-query) 0)
          total-pages (if (pos? total-items) (int (Math/ceil (/ (double total-items) limit))) 0)
          folders (execute! folders-query)]

      (resp/response {:folders folders
                      :pagination {:page page
                                   :limit limit
                                   :total_items total-items
                                   :total_pages total-pages}}))
    (catch Exception e
      (server-error "Failed to retrieve public folders." e))))

(defn get-folder-details-handler [request]
  (let [folder-id (parse-query-param (:path-params request) :folder-id :int)]
    (if-not folder-id
      (bad-request "Invalid Folder ID." nil)
      (if-let [user-id (get-user-id request)] ; Check if user is authenticated
        (try
          (let [folder-details (execute-one!
                                (-> (hh/select :f.folder_id :f.name :f.description :f.is_public :f.user_id :f.created_at :f.updated_at
                                               :u.email :u.profile_picture_url) ; Select email instead of username
                                    (hh/from [:folders :f])
                                    (hh/join [:users :u] [:= :f.user_id :u.id]) ; Join on u.id
                                    (hh/where [:= :f.folder_id folder-id])))]
            (if folder-details
              (if (or (:is-public folder-details) (= (:user-id folder-details) user-id))
                (let [question-sets (execute!
                                     (-> (hh/select :qs.* :fqs.order_in_folder :fqs.added_at
                                                    [[:coalesce [:count [:distinct [:case [:<> :ua.answer_id nil] :q.question_id]]] 0] :answered_count]
                                                    [[:coalesce [:count [:distinct [:case [:= :ua.is_correct 1] :q.question_id]]] 0] :correct_count]
                                                    [[:count [:distinct :q.question_id]] :total_questions])
                                         (hh/from [:folder_question_sets :fqs])
                                         (hh/join [:question_sets :qs] [:= :fqs.set_id :qs.set_id])
                                         (hh/left-join [:questions :q] [:= :qs.set_id :q.set_id])
                                         (hh/left-join [:user_answers :ua] [:and [:= :q.question_id :ua.question_id] [:= :ua.user_id user-id]])
                                         (hh/where [:= :fqs.folder_id folder-id])
                                         (hh/group-by :qs.set_id :fqs.order_in_folder :fqs.added_at)
                                         (hh/order-by [:fqs.order_in_folder :asc] [:qs.title :asc])))]
                  (resp/response (assoc folder-details :question-sets (mapv (fn [s]
                                                                              (let [total (:total-questions s 0)
                                                                                    answered (:answered-count s 0)
                                                                                    correct (:correct-count s 0)]
                                                                                (assoc s :progress {:total total
                                                                                                    :answered answered
                                                                                                    :correct correct
                                                                                                    :answered-percent (if (pos? total) (double (/ answered total)) 0.0)
                                                                                                    :correct-percent (if (pos? answered) (double (/ correct answered)) 0.0)}))) question-sets))))
                (unauthorized "You do not have permission to view this folder."))
              (not-found "Folder not found.")))
          (catch Exception e
            (server-error (str "Failed to retrieve folder details for ID: " folder-id) e)))
        ;; If user is not authenticated, only allow access to public folders
        (try
          (let [folder-details (execute-one!
                                (-> (hh/select :f.folder_id :f.name :f.description :f.is_public :f.user_id :f.created_at :f.updated_at
                                               :u.email :u.profile_picture_url) ; Select email instead of username
                                    (hh/from [:folders :f])
                                    (hh/join [:users :u] [:= :f.user_id :u.id]) ; Join on u.id
                                    (hh/where [:and [:= :f.folder_id folder-id] [:= :f.is_public true]])))]
            (if folder-details
              (let [question-sets (execute!
                                   (-> (hh/select :qs.* :fqs.order_in_folder :fqs.added_at
                                                  [[:count [:distinct :q.question_id]] :total_questions]) ; No progress for anonymous users
                                       (hh/from [:folder_question_sets :fqs])
                                       (hh/join [:question_sets :qs] [:= :fqs.set_id :qs.set_id])
                                       (hh/left-join [:questions :q] [:= :qs.set_id :q.set_id])
                                       (hh/where [:= :fqs.folder_id folder-id])
                                       (hh/group-by :qs.set_id :fqs.order_in_folder :fqs.added_at)
                                       (hh/order-by [:fqs.order_in_folder :asc] [:qs.title :asc])))]
                (resp/response (assoc folder-details :question-sets (mapv (fn [s]
                                                                            (assoc s :progress {:total (:total-questions s 0)
                                                                                                :answered 0
                                                                                                :correct 0
                                                                                                :answered-percent 0.0
                                                                                                :correct-percent 0.0})) question-sets))))
              (not-found "Public folder not found or you do not have permission.")))
          (catch Exception e
            (server-error (str "Failed to retrieve public folder details for ID: " folder-id) e)))))))

(defn update-folder-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [folder-id (parse-query-param (:path-params request) :folder-id :int)
          {:keys [name description is-public] :as body-params} (:body-params request)]
      (cond
        (not folder-id) (bad-request "Invalid Folder ID." nil)
        (empty? (select-keys body-params [:name :description :is-public])) (bad-request "No update data provided." nil)
        (and (contains? body-params :name) (str/blank? name)) (bad-request "Folder name cannot be empty." nil)
        (and (contains? body-params :is-public) (not (boolean? is-public))) (bad-request "is-public must be a boolean." nil)
        :else
        (try
          (let [folder (execute-one! (-> (hh/select :user_id) (hh/from :folders) (hh/where [:= :folder_id folder-id])))]
            (if folder
              (if (= (:user-id folder) user-id)
                (let [update-payload (cond-> {}
                                       (contains? body-params :name) (assoc :name name)
                                       (contains? body-params :description) (assoc :description description)
                                       (contains? body-params :is-public) (assoc :is-public is-public))
                      _ (execute-one! (-> (hh/update :folders)
                                          (hh/set update-payload)
                                          (hh/where [:= :folder_id folder-id])))
                      updated-folder (execute-one! (-> (hh/select :*) (hh/from :folders) (hh/where [:= :folder_id folder-id])))]
                  (resp/response updated-folder))
                (unauthorized "You do not have permission to update this folder."))
              (not-found "Folder not found.")))
          (catch Exception e
            (server-error (str "Failed to update folder ID: " folder-id) e)))))
    (unauthorized "Authentication required to update a folder.")))

(defn delete-folder-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [folder-id (parse-query-param (:path-params request) :folder-id :int)]
      (if-not folder-id
        (bad-request "Invalid Folder ID." nil)
        (jdbc/with-transaction [tx @db-pool jdbc-opts]
          (try
            (let [folder (jdbc/execute-one! tx (h/format (-> (hh/select :user_id) (hh/from :folders) (hh/where [:= :folder_id folder-id]))) jdbc-opts)]
              (if folder
                (if (= (:user-id folder) user-id)
                  (do
                    (jdbc/execute! tx (h/format (-> (hh/delete-from :folder_question_sets) (hh/where [:= :folder_id folder-id]))) jdbc-opts) ; Delete associations first
                    (let [deleted-count (:next.jdbc/update-count (jdbc/execute! tx (h/format (-> (hh/delete-from :folders) (hh/where [:= :folder_id folder-id]))) jdbc-opts))]
                      (resp/response {:success true :deleted-count deleted-count})))
                  (unauthorized "You do not have permission to delete this folder."))
                (not-found "Folder not found.")))
            (catch Exception e
              (.rollback tx)
              (server-error (str "Failed to delete folder ID: " folder-id) e))))))
    (unauthorized "Authentication required to delete a folder.")))

(defn add-set-to-folder-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [folder-id (parse-query-param (:path-params request) :folder-id :int)
          {:keys [sets] :as _body} (:body-params request)] ; Changed to expect a list of sets
      (cond
        (not folder-id) (bad-request "Invalid Folder ID." nil)
        (or (nil? sets) (not (seq sets)) (not (vector? sets)))
        (bad-request "Missing or invalid 'sets' array in request body. Expecting [{:set-id ..., :order-in-folder ...}, ...]." nil)

        (not (every? (fn [s] (and (map? s)
                                  (integer? (:set-id s))
                                  (integer? (:order-in-folder s))))
                     sets))
        (bad-request "Each item in 'sets' array must be an object with integer 'set-id' and 'order-in-folder'." nil)

        :else
        (jdbc/with-transaction [tx @db-pool jdbc-opts]
          (try
            (let [folder (jdbc/execute-one! tx (h/format (-> (hh/select :user_id) (hh/from :folders) (hh/where [:= :folder_id folder-id]))) jdbc-opts)]
              (if folder
                (if (= (:user-id folder) user-id)
                  (do
                    (doseq [set-item sets]
                      (let [current-set-id (:set-id set-item)
                            current-order-in-folder (:order-in-folder set-item)
                            question-set (jdbc/execute-one! tx (h/format (-> (hh/select :set_id) (hh/from :question_sets) (hh/where [:= :set_id current-set-id]))) jdbc-opts)]
                        (if question-set
                          ;; Assuming order-in-folder is now always provided by the client for batch operations
                          (jdbc/execute! tx (h/format (-> (hh/insert-into :folder_question_sets)
                                                          (hh/values [{:folder_id folder-id
                                                                       :set_id current-set-id
                                                                       :order_in_folder current-order-in-folder}])))
                                         jdbc-opts)
                          (throw (ex-info (str "Question set with ID " current-set-id " not found.") {:type :not-found :set-id current-set-id})))))
                    (resp/response {:success true :folder-id folder-id :added-count (count sets)}))
                  (unauthorized "You do not have permission to modify this folder."))
                (not-found "Folder not found.")))
            (catch Exception e
              (.rollback tx)
              (if (and (ex-data e) (= :not-found (:type (ex-data e))))
                (not-found (ex-message e))
                (if (str/includes? (or (.getMessage e) "") "UNIQUE constraint failed: folder_question_sets.folder_id, folder_question_sets.set_id")
                  (bad-request "One or more question sets already exist in this folder." {:folder-id folder-id :sets sets})
                  (server-error (str "Failed to add sets to folder. F_ID: " folder-id) e))))))))
    (unauthorized "Authentication required to add set to folder.")))

(defn remove-set-from-folder-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [folder-id (parse-query-param (:path-params request) :folder-id :int)
          set-id (parse-query-param (:path-params request) :set-id :int)]
      (cond
        (not folder-id) (bad-request "Invalid Folder ID." nil)
        (not set-id) (bad-request "Invalid Set ID." nil)
        :else
        (jdbc/with-transaction [tx @db-pool jdbc-opts]
          (try
            (let [folder (jdbc/execute-one! tx (h/format (-> (hh/select :user_id) (hh/from :folders) (hh/where [:= :folder_id folder-id]))) jdbc-opts)]
              (if folder
                (if (= (:user-id folder) user-id)
                  (let [deleted-count (:next.jdbc/update-count (jdbc/execute! tx (h/format (-> (hh/delete-from :folder_question_sets)
                                                                                               (hh/where [:and [:= :folder_id folder-id] [:= :set_id set-id]]))) jdbc-opts))]
                    (resp/response {:success true :deleted-count deleted-count}))
                  (unauthorized "You do not have permission to modify this folder."))
                (not-found "Folder not found.")))
            (catch Exception e
              (.rollback tx)
              (server-error (str "Failed to remove set from folder. F_ID: " folder-id " S_ID: " set-id) e))))))
    (unauthorized "Authentication required to remove set from folder.")))

(defn reorder-sets-in-folder-handler [request]
  (if-let [user-id (get-user-id request)]
    (let [folder-id (parse-query-param (:path-params request) :folder-id :int)
          ordered-sets (:sets (:body-params request))] ; Expecting a list of {:set_id int :order_in_folder int}
      (cond
        (not folder-id) (bad-request "Invalid Folder ID." nil)
        (not (seq ordered-sets)) (bad-request "Missing or empty 'sets' array in request body." nil)
        (not (every? (fn [s] (and (integer? (:set-id s)) (integer? (:order-in-folder s)))) ordered-sets))
        (bad-request "Each item in 'sets' array must have integer 'set-id' and 'order-in-folder'." nil)
        :else
        (jdbc/with-transaction [tx @db-pool jdbc-opts]
          (try
            (let [folder (jdbc/execute-one! tx (h/format (-> (hh/select :user_id) (hh/from :folders) (hh/where [:= :folder_id folder-id]))) jdbc-opts)]
              (if folder
                (if (= (:user-id folder) user-id)
                  (do
                    (doseq [s ordered-sets]
                      (jdbc/execute! tx (h/format (-> (hh/update :folder_question_sets)
                                                      (hh/set {:order_in_folder (:order-in-folder s)})
                                                      (hh/where [:and [:= :folder_id folder-id] [:= :set_id (:set-id s)]]))) jdbc-opts))
                    (resp/response {:success true}))
                  (unauthorized "You do not have permission to reorder sets in this folder."))
                (not-found "Folder not found.")))
            (catch Exception e
              (.rollback tx)
              (server-error (str "Failed to reorder sets in folder ID: " folder-id) e))))))
    (unauthorized "Authentication required to reorder sets.")))

(comment
  (sql/query @db-pool ["SELECT * FROM users"])
  (sql/query @db-pool ["DELETE FROM users WHERE id = 2"])

  ;; --- Migration script to convert existing question_data keys to kebab-case ---
  (require '[clojure.walk :as walk])

  (defn- transform-keys-to-kebab [data]
    (letfn [(kebab-key [k]
              (if (keyword? k)
                (keyword (str/replace (name k) "_" "-"))
                k))]
      (walk/postwalk
       (fn [x]
         (if (map? x)
           (into {} (map (fn [[k v]] [(kebab-key k) v]) x))
           x))
       data)))

  (defn- migrate-question-data-to-kebab-case [db-connection]
    (println "Starting migration of question_data to kebab-case...")
    (let [questions (jdbc/execute! db-connection
                                   ["SELECT question_id, question_data FROM questions"]
                                   {:builder-fn rs/as-unqualified-kebab-maps})]
      (println (str "Found " (count questions) " questions to process."))
      (doseq [question questions]
        (let [question-id (:question-id question)
              old-data-str (:question-data question)]
          (when-not (str/blank? old-data-str)
            (try
              (let [parsed-data (edn/read-string old-data-str)
                    kebab-cased-data (transform-keys-to-kebab parsed-data)
                    new-data-str (pr-str kebab-cased-data)]
                (if (= old-data-str new-data-str)
                  (println (str "Question ID: " question-id " - data already kebab-case or no underscore keys found."))
                  (do
                    (println (str "Migrating Question ID: " question-id))
                    (jdbc/execute-one! db-connection
                                       ["UPDATE questions SET question_data = ? WHERE question_id = ?"
                                        new-data-str question-id]))))
              (catch Exception e
                (println (str "ERROR processing Question ID: " question-id ". Error: " (ex-message e)))
                (println (str "Problematic data string: " old-data-str)))))))
      (println "Migration completed.")))

  (jdbc/with-transaction [tx @db-pool]
    (migrate-question-data-to-kebab-case tx)))


(comment
  (->> (sql/query @db-pool ["SELECT * FROM questions_fts"])
      (reverse)
      (take 50))

  (jdbc/with-transaction [tx @db-pool {:builder-fn rs/as-unqualified-kebab-maps}]
    (let [questions (jdbc/execute! tx ["SELECT question_id, set_id, question_type, question_data, retention_aid FROM questions"] jdbc-opts)]
      (doseq [q questions]
        (fts/update-question-fts! tx
                                  (:question-id q)
                                  (:set-id q)
                                  (:question-type q)
                                  (:question-data q)
                                  (:retention-aid q)))
      (println "Updated FTS index for" (count questions) "questions")))


  (try
    (let [results (jdbc/execute! @db-pool ["SELECT question_id FROM questions_fts WHERE searchable_text MATCH ?" "erect"])]
      (println "Raw FTS query results:" results))
    (catch Exception e (println "Raw FTS query failed:" (ex-message e) (.printStackTrace e)))))