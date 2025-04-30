(ns zi-study.frontend.utilities.http
  (:require [goog.object :as gobj]
            [zi-study.frontend.utilities.auth-core :as auth-core]))

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