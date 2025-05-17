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

(defn build-query-params [params]
  (let [processed-params (->> params
                              (filter (fn [[_ v]]
                                        (if (coll? v)
                                          (seq v)
                                          (some? v))))
                              (map (fn [[k v]]
                                     (let [encoded-key (js/encodeURIComponent (name k))]
                                       (if (coll? v)
                                         (str encoded-key "=" (->> v (map (comp js/encodeURIComponent str)) (str/join ",")))
                                         (str encoded-key "=" (js/encodeURIComponent (str v))))))))]
    (when (seq processed-params)
      (str/join "&" processed-params))))

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
                    (callback {:success true :data (:data result)}))
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
                    (state/set-current-set-question-ids (:questions (:data result)))
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
                       ;; Get the question directly from the registry
                       question (get-in @state/app-state [:questions-registry question-id])
                       question-data (:question-data question)
                       final-is-correct (cond
                                          (= question-type "written") nil

                                          (= question-type "mcq-single")
                                          (if (= (:correct-index question-data) (:answer answer-data)) 1 0)

                                          :else
                                          (let [is-correct-from-backend (:correct data)]
                                            (if (nil? is-correct-from-backend) nil (if is-correct-from-backend 1 0))))
                       updated-question-data {:user-answer {:answer-data answer-data
                                                            :is-correct final-is-correct
                                                            :submitted-at (js/Date.)}
                                              :bookmarked (:bookmarked question)}]

                   ;; Update in the registry only, no need to update separate lists
                   (state/update-question-in-registry question-id updated-question-data)
                   (state/set-answer-submitting question-id false nil)
                   (callback {:success true :data data}))
                 (do
                   (state/set-answer-submitting question-id false (:error result))
                   (callback {:success false :error (:error result)}))))))

(defn self-evaluate [question-id is-correct callback]
  (state/set-self-evaluating question-id true)
  (post-auth (str "/api/questions/" question-id "/self-evaluate")
             {:is-correct is-correct}
             (fn [result]
               (if (:success result)
                 (do
                   (let [;; Get the question directly from the registry
                         question (get-in @state/app-state [:questions-registry question-id])
                         updated-user-answer (assoc (:user-answer question) :is-correct (if is-correct 1 0))]

                     ;; Update in the registry only
                     (state/update-question-in-registry question-id
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
                     ;; Update in the registry only
                     (state/update-question-in-registry question-id {:user-answer nil})
                     (state/set-answer-submitting question-id false nil)
                     (callback {:success true
                                :deleted-count (get-in result [:data :deleted-count])}))
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
                     (js/console.log "Successfully deleted" (or (:deleted-count (:data result)) 0) "answers")
                     ;; After deletion, reload the questions with current filters
                     (let [filters @(state/get-current-set-filters)]
                       (get-set-questions
                        set-id
                        filters
                        (fn [result]
                          (if (:success result)
                            (callback {:success true :deleted-count (:deleted-count (:data result))})
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
                   ;; Update in registry only
                   (state/update-question-in-registry question-id {:bookmarked new-bookmarked-state})
                   (state/set-bookmark-toggling question-id false)
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

(defn advanced-search-questions [filters pagination callback]
  (let [current-page (or (:page pagination) 1)
        current-limit (or (:limit pagination) 15)
        query-params (cond-> {:keywords (:keywords filters)
                              :page current-page
                              :limit current-limit}
                       ;; Add other optional filters here if they exist in `filters`
                       ;; e.g. (:tags filters) (assoc :tags (str/join "," (:tags filters)))
                       )
        query-string (build-query-params query-params)]

    (state/set-advanced-search-loading true)
    (get-auth (str "/api/questions/search" (when query-string (str "?" query-string)))
              (fn [result]
                (if (:success result)
                  (do
                    ;; Use the new set-advanced-search-question-ids function
                    (state/set-advanced-search-question-ids (:questions (:data result)) (:pagination (:data result)))
                    (callback {:success true :data (:data result)}))
                  (do
                    (state/set-advanced-search-error (:error result))
                    (callback {:success false :error (:error result)})))))))

; --- Folder API Calls ---

(defn create-folder
  "Creates a new folder and adds it to the user's folder list"
  [folder-data success-callback error-callback]
  (post-auth "/api/folders"
             folder-data
             (fn [result]
               (if (:success result)
                 (let [created-folder (:data result)]
                   (state/add-folder-to-user-list created-folder)
                   (when success-callback (success-callback created-folder)))
                 (let [error-msg (get-in result [:data :error] (:error result "Failed to create folder."))]
                   (when error-callback (error-callback error-msg (:data result))))))))

(defn get-user-folders [callback]
  (state/set-user-folders-loading true)
  (get-auth "/api/folders"
            (fn [result]
              (if (:success result)
                (do
                  (state/set-user-folders-list (:folders (:data result)))
                  (when callback (callback {:success true :data (:data result)})))
                (let [error-msg (get-in result [:data :error] (:error result "Failed to load your folders."))]
                  (state/set-user-folders-error error-msg)
                  (when callback (callback {:success false :error error-msg :data (:data result)})))))))

(defn get-public-folders [params callback]
  (state/set-public-folders-loading true)
  (let [query-string (build-query-params params)
        url (str "/api/folders/public" (when query-string (str "?" query-string)))]
    (get-auth url
              (fn [result]
                (if (:success result)
                  (let [data (:data result)]
                    (state/set-public-folders-list (:folders data) (:pagination data))
                    (when callback (callback {:success true :data data})))
                  (let [error-msg (get-in result [:data :error] (:error result "Failed to load public folders."))]
                    (state/set-public-folders-error error-msg)
                    (when callback (callback {:success false :error error-msg :data (:data result)}))))))))

(defn get-folder-details [folder-id callback]
  (state/set-current-folder-details-loading true)
  (get-auth (str "/api/folders/folder/" folder-id)
            (fn [result]
              (if (:success result)
                (do
                  (state/set-current-folder-details (:data result))
                  (when callback (callback {:success true :data (:data result)})))
                (let [error-msg (get-in result [:data :error] (:error result "Failed to load folder."))]
                  (state/set-current-folder-details-error error-msg)
                  (when callback (callback {:success false :error error-msg :data (:data result)})))))))

(defn update-folder [folder-id update-data success-callback error-callback]
  (put-auth (str "/api/folders/folder/" folder-id)
            update-data
            (fn [result]
              (if (:success result)
                (let [response-data (:data result)]
                  (state/update-folder-in-user-list response-data)
                  (when (= (or (:folder-id (:details @(state/get-current-folder-details-state)))
                               (:folder_id (:details @(state/get-current-folder-details-state))))
                           folder-id)
                    (state/set-current-folder-details response-data))
                  (when success-callback (success-callback response-data)))
                (let [error-msg (get-in result [:data :error] (:error result "Failed to update folder."))]
                  (when error-callback (error-callback error-msg (:data result))))))))

(defn delete-folder [folder-id success-callback error-callback]
  (delete-auth (str "/api/folders/folder/" folder-id)
               (fn [result]
                 (if (:success result)
                   (let [response-data (:data result)]
                     (state/remove-folder-from-user-list folder-id)
                     (when (= (or (:folder-id (:details @(state/get-current-folder-details-state)))
                                  (:folder_id (:details @(state/get-current-folder-details-state))))
                              folder-id)
                       (state/clear-current-folder-details))
                     (when success-callback (success-callback response-data)))
                   (let [error-msg (get-in result [:data :error] (:error result "Failed to delete folder."))]
                     (when error-callback (error-callback error-msg (:data result))))))))

(defn add-set-to-folder [folder-id set-id order success-callback error-callback]
  (state/set-folder-managing-sets-state folder-id true nil)
  (post-auth (str "/api/folders/folder/" folder-id "/sets")
             {:set-id set-id :order-in-folder order}
             (fn [result]
               (state/set-folder-managing-sets-state folder-id false (when-not (:success result) (:error result)))
               (if (:success result)
                 (let [response-data (:data result)]
                   (state/add-set-to-current-folder-details response-data) ; Assuming backend returns the added set or full folder
                   (when success-callback (success-callback response-data)))
                 (let [error-msg (get-in result [:data :error] (:error result "Failed to add set to folder."))]
                   (when error-callback (error-callback error-msg (:data result))))))))

(defn remove-set-from-folder [folder-id set-id success-callback error-callback]
  (state/set-folder-managing-sets-state folder-id true nil)
  (delete-auth (str "/api/folders/folder/" folder-id "/sets/" set-id)
               (fn [result]
                 (state/set-folder-managing-sets-state folder-id false (when-not (:success result) (:error result)))
                 (if (:success result)
                   (let [response-data (:data result)]
                     (state/remove-set-from-current-folder-details set-id)
                     (when success-callback (success-callback response-data)))
                   (let [error-msg (get-in result [:data :error] (:error result "Failed to remove set from folder."))]
                     (when error-callback (error-callback error-msg (:data result))))))))

(defn reorder-sets-in-folder [folder-id ordered-sets-data success-callback error-callback]
  (state/set-folder-managing-sets-state folder-id true nil)
  (put-auth (str "/api/folders/folder/" folder-id "/sets")
            {:sets ordered-sets-data}
            (fn [result]
              (state/set-folder-managing-sets-state folder-id false (when-not (:success result) (:error result)))
              (if (:success result)
                (let [response-data (:data result)]
                  (when success-callback (success-callback response-data)))
                (let [error-msg (get-in result [:data :error] (:error result "Failed to reorder sets."))]
                  (when error-callback (error-callback error-msg (:data result))))))))

