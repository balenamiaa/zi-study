(ns zi-study.frontend.utilities.http
  (:require [goog.object :as gobj]
            [zi-study.frontend.utilities.auth-core :as auth-core]
            [zi-study.frontend.state :as state]
            [clojure.string :as str]))

(defn json-request
  "Creates a request object with JSON content type and optional body"
  [method body]
  (let [request (clj->js {:method method
                          :headers {"Content-Type" "application/json"}})]
    (when body
      (gobj/set request "body" (js/JSON.stringify (clj->js body))))
    request))

(defn fetch
  "Perform a fetch request with standard error handling"
  [url options callback]
  (-> (js/fetch url (clj->js options))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (js/Promise.reject
                  (or (.-statusText response)
                      (str "Error: " (.-status response)))))))
      (.then (fn [data]
               (callback {:success true
                          :data (js->clj data :keywordize-keys true)})))
      (.catch (fn [err]
                (callback {:success false
                           :error (if (string? err)
                                    err
                                    (or (.-message err)
                                        "An unexpected error occurred."))})))))

(defn fetch-auth
  "Perform an authenticated fetch request"
  [url options callback]
  (let [token (auth-core/get-token)
        js-options (clj->js options)
        headers (or (gobj/get js-options "headers") #js {})]
    (when token
      (gobj/set headers "Authorization" (str "Bearer " token)))
    (gobj/set js-options "headers" headers)
    (-> (js/fetch url js-options)
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (js/Promise.reject
                    (or (.-statusText response)
                        (str "Error: " (.-status response)))))))
        (.then (fn [data]
                 (callback {:success true
                            :data (js->clj data :keywordize-keys true)})))
        (.catch (fn [err]
                  (js/console.error err)
                  (callback {:success false
                             :error (if (string? err)
                                      err
                                      (or (.-message err)
                                          "An unexpected error occurred."))}))))))

(defn get-req
  "GET request with standard error handling"
  [url callback]
  (fetch url (clj->js {:method "GET"}) callback))

(defn get-auth
  "Authenticated GET request"
  [url callback]
  (fetch-auth url (clj->js {:method "GET"}) callback))

(defn post-req
  "POST request with JSON payload"
  [url data callback]
  (fetch url (json-request "POST" data) callback))

(defn post-auth
  "Authenticated POST request with JSON payload"
  [url data callback]
  (fetch-auth url (json-request "POST" data) callback))

(defn put-req
  "PUT request with JSON payload"
  [url data callback]
  (fetch url (json-request "PUT" data) callback))

(defn put-auth
  "Authenticated PUT request with JSON payload"
  [url data callback]
  (fetch-auth url (json-request "PUT" data) callback))

(defn delete-req
  "DELETE request"
  [url callback]
  (fetch url (clj->js {:method "DELETE"}) callback))

(defn delete-auth
  "Authenticated DELETE request"
  [url callback]
  (fetch-auth url (clj->js {:method "DELETE"}) callback))

;; --- Question Bank API Client Functions ---

(defn- build-query-params [params]
  (->> params
       (filter (fn [[_ v]] (if (coll? v) (seq v) (some? v))))
       (map (fn [[k v]]
              (let [param-name (name k)]
                (if (coll? v)
                  (str param-name "=" (str/join "," v))
                  (str param-name "=" v)))))
       (str/join "&")
       (not-empty)))

(defn get-tags [callback]
  (state/set-tags-loading true)
  (get-auth "/api/tags"
            (fn [result]
              (if (:success result)
                (do
                  (state/set-tags-list (:tags (:data result)))
                  (callback {:success true}))
                (do
                  (state/set-tags-error (:error result))
                  (callback {:success false :error (:error result)}))))))

(defn get-sets [filters pagination callback]
  (state/set-sets-loading true)
  (let [params (merge filters pagination)
        query-string (build-query-params params)
        url (str "/api/question-sets" (when query-string (str "?" query-string)))]
    (get-auth url
              (fn [result]
                (if (:success result)
                  (let [data (:data result)]
                    (state/set-sets-list (:sets data) (:pagination data))
                    (callback {:success true}))
                  (do
                    (state/set-sets-error (:error result))
                    (callback {:success false :error (:error result)})))))))

(defn get-set-details [set-id callback]
  (state/set-current-set-loading true)
  (get-auth (str "/api/question-sets/" set-id)
            (fn [result]
              (if (:success result)
                (callback {:success true :data (:data result)})
                (do
                  (state/set-current-set-error (:error result))
                  (callback {:success false :error (:error result)}))))))

(defn get-set-questions [set-id filters callback]
  (state/set-current-set-questions-loading true)
  (let [query-string (build-query-params filters)
        url (str "/api/question-sets/" set-id "/questions" (when query-string (str "?" query-string)))]
    (get-auth url
              (fn [result]
                (if (:success result)
                  (do
                    (state/set-current-set-questions-loading false)
                    (callback {:success true :data (:data result)}))
                  (do
                    (state/set-current-set-questions-loading false)
                    (state/set-current-set-questions-error (:error result))
                    (callback {:success false :error (:error result)})))))))

(defn submit-answer [question-id question-type answer-data callback]
  (state/set-answer-submitting question-id true nil)
  (post-auth (str "/api/questions/" question-id "/answer")
             answer-data
             (fn [result]
               (if (:success result)
                 (let [data (:data result)
                       current-question (first (filter #(= (:question-id %) question-id) @(state/get-current-set-questions)))
                       question-data (:question-data current-question)
                       final-is-correct (cond
                                          (= question-type "written") nil

                                          (= question-type "mcq-single")
                                          (if (= (:correct_index question-data) (:answer answer-data)) 1 0)

                                          :else
                                          (let [is-correct-from-backend (:correct data)]
                                            (if (nil? is-correct-from-backend) nil (if is-correct-from-backend 1 0))))]
                   (state/update-current-question question-id
                                                  {:user-answer {:answer-data answer-data
                                                                 :is-correct final-is-correct
                                                                 :submitted-at (js/Date.)}
                                                   :bookmarked (:bookmarked current-question)})
                   (state/set-answer-submitting question-id false nil)
                   (callback {:success true :data data}))
                 (do
                   (state/set-answer-submitting question-id false (:error result))
                   (callback {:success false :error (:error result)}))))))

(defn self-evaluate [question-id is-correct callback]
  (state/set-self-evaluating question-id true)
  (post-auth (str "/api/questions/" question-id "/self-evaluate")
             {:is_correct is-correct}
             (fn [result]
               (if (:success result)
                 (do
                   (let [current-questions @(state/get-current-set-questions)
                         question-to-update (first (filter #(= (:question-id %) question-id) current-questions))
                         updated-user-answer (assoc (:user-answer question-to-update) :is-correct (if is-correct 1 0))]
                     (state/update-current-question question-id
                                                    {:user-answer updated-user-answer}))
                   (state/set-self-evaluating question-id false)
                   (callback {:success true}))
                 (do
                   (state/set-self-evaluating question-id false)
                   (callback {:success false :error (:error result)}))))))

(defn delete-answer
  "Deletes a single answer for a question"
  [question-id callback]
  (state/set-answer-submitting question-id true nil)
  (delete-auth (str "/api/questions/" question-id "/answer")
               (fn [result]
                 (if (:success result)
                   (do
                     ;; First immediately update the local state for instant feedback
                     (state/update-current-question question-id
                                                    {:user-answer nil})
                     (state/set-answer-submitting question-id false nil)

                     ;; Then inform the caller
                     (callback {:success true
                                :deleted_count (get-in result [:data :deleted_count])}))
                   (do
                     (state/set-answer-submitting question-id false (:error result))
                     (callback {:success false :error (:error result)}))))))

(defn delete-answers
  "Deletes all answers for a question set"
  [set-id callback]
  (state/set-current-set-questions-loading true)
  (delete-auth (str "/api/question-sets/" set-id "/answers")
               (fn [result]
                 (if (:success result)
                   (do
                     (js/console.log "Successfully deleted" (or (:deleted_count (:data result)) 0) "answers")
                     ;; After deletion, reload the questions with current filters to reflect the changes
                     (let [filters @(state/get-current-set-filters)]
                       (get-set-questions
                        set-id
                        filters
                        (fn [result]
                          (if (:success result)
                            (do
                              (state/set-current-set-questions (:questions (:data result)))
                              (callback {:success true :deleted_count (:deleted_count (:data result))}))
                            (do
                              (state/set-current-set-questions-loading false)
                              (state/set-current-set-questions-error (:error result))
                              (callback {:success false :error (:error result)})))))))
                   (do
                     (state/set-current-set-questions-loading false)
                     (callback {:success false :error (:error result)}))))))

(defn toggle-bookmark [question-id bookmarked callback]
  (state/set-bookmark-toggling question-id true)
  (post-auth (str "/api/questions/" question-id "/bookmark")
             {:bookmarked bookmarked}
             (fn [result]
               (if (:success result)
                 (let [new-bookmarked-state (:bookmarked (:data result))]
                   (state/set-bookmark-toggling question-id false)
                   (state/update-current-question question-id {:bookmarked new-bookmarked-state})
                   (callback {:success true :bookmarked new-bookmarked-state}))
                 (do
                   (state/set-bookmark-toggling question-id false)
                   (println "Bookmark toggle failed:" (:error result))
                   (callback {:success false :error (:error result)}))))))

(defn get-bookmarks [callback]
  (state/set-bookmarks-loading true)
  (get-auth "/api/bookmarks"
            (fn [result]
              (if (:success result)
                (do
                  (state/set-bookmarks-list (:bookmarks (:data result)))
                  (callback {:success true}))
                (do
                  (state/set-bookmarks-error (:error result))
                  (callback {:success false :error (:error result)}))))))


