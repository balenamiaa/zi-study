(ns zi-study.frontend.utilities.auth
  (:require [zi-study.frontend.utilities.auth-core :as auth-core]
            [zi-study.frontend.utilities.http :as http]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.state :as state]
            [goog.object :as gobj]))



(defn login
  "Login user with email and password"
  [email password callback]
  (-> (js/fetch "/api/auth/login"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/json"}
                          :body (js/JSON.stringify #js {:email email :password password})}))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (js/Promise.reject "Invalid email or password"))))
      (.then (fn [data]
               (when-let [token (gobj/get data "token")]
                 (let [user (gobj/get data "user")]
                   (auth-core/set-token token)
                   (callback {:success true
                              :token token
                              :user (js->clj user :keywordize-keys true)})))))
      (.catch (fn [err]
                (callback {:success false
                           :error (if (string? err)
                                    err
                                    (or (.-message err) "An unexpected error occurred."))})))))

(defn register
  "Register a new user"
  [user-data callback]
  (-> (js/fetch "/api/auth/register"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/json"}
                          :body (js/JSON.stringify (clj->js user-data))}))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (js/Promise.reject "Registration failed"))))
      (.then (fn [data]
               (callback {:success true
                          :data (js->clj data :keywordize-keys true)})))
      (.catch (fn [err]
                (callback {:success false
                           :error (if (string? err)
                                    err
                                    (or (.-message err) "An unexpected error occurred."))})))))

(defn get-current-user
  "Get the current authenticated user. Redirects to login on failure."
  [callback]
  (if (auth-core/get-token)
    (http/fetch-auth "/api/auth/me"
                     {:method "GET"}
                     (fn [result]
                       (if (:success result)
                         (when-let [user (:user (:data result))]
                           (callback {:success true
                                      :user user}))
                         (do
                           (auth-core/remove-token)
                           (state/reset-auth-state!)
                           (rfe/push-state :zi-study.frontend.core/login)
                           (callback {:success false
                                      :error "Authentication failed"})))))
    (callback {:success false
               :error "No token found"})))

(defn logout
  "Logout the current user. Calls backend to clear session cookie and removes local token."
  [callback]
  (auth-core/remove-token)
  (state/reset-auth-state!)
  
  (http/fetch-auth "/api/auth/logout"
                   {:method "POST"}
                   (fn [result]
                     (if (:success result)
                       (do
                         (callback {:success true}))
                       (do
                         (println "Logout: Server call to clear session failed, but locally logged out.")
                         (callback {:success true :warning "Server session might still be active."}))))))