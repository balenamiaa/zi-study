(ns zi-study.frontend.core
  (:require [reagent.dom.client :as rdom]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [zi-study.frontend.utilities.cookies :as cookies]
            [zi-study.frontend.pages.home :refer [home-page]]
            [zi-study.frontend.pages.components :refer [components-page]]
            [zi-study.frontend.pages.not-found :refer [not-found-page]]
            [zi-study.frontend.pages.login :refer [login-page]]
            [zi-study.frontend.pages.register :refer [register-page]]
            [zi-study.frontend.pages.question-sets :refer [question-sets-page]]
            [zi-study.frontend.pages.set-page :refer [set-page]]
            [zi-study.frontend.layouts.main-layout :refer [main-layout]]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.auth :as auth]
            [zi-study.frontend.utilities.auth-core :as auth-core]
            [zi-study.frontend.utilities.auth-guard :as auth-guard]
            [zi-study.frontend.utilities.theme :as theme]))



(def routes
  [["/"
    {:name ::home
     :view home-page}]

   ["/components"
    {:name ::components
     :view components-page}]

   ["/login"
    {:name ::login
     :view login-page}]

   ["/register"
    {:name ::register
     :view register-page}]

   ["/question-sets"
    {:name ::question-sets
     :view (auth-guard/with-auth {:content question-sets-page
                                  :redirect-to ::login
                                  :message "Please log in to view the question sets"})}]

   ["/question-sets/:set-id"
    {:name ::set-page
     :view (auth-guard/with-auth {:content set-page
                                  :redirect-to ::login
                                  :message "Please log in to view the question set"})
     :parameters {:path {:set-id int?}}}]])


(defn fetch-current-user-details
  "Fetches current user details if a token exists."
  []
  (let [token (auth-core/get-token)]
    (when token
      (state/set-auth-loading-current-user true)
      (auth/get-current-user
       (fn [result]
         (if (:success result)
           (state/set-fully-authenticated token (:user result))
           (do
             (auth-core/remove-token)
             (state/set-unauthenticated))))))))

(defn initial-auth-check
  "Performs the initial, faster authentication check using local token and cookie."
  []
  (let [token (auth-core/get-token)
        session-cookie (cookies/get-cookie "auth-session-active")]
    (if (and token session-cookie)
      (do
        (state/set-provisionally-authenticated token)
        (fetch-current-user-details)) ; Now fetch full details
      (state/set-unauthenticated))))


(defn app []
  (let [current-match (state/get-current-route)
        current-route (get-in current-match [:data :name])]
    [main-layout
     {:current-route current-route
      :children (if current-match
                  (let [view (get-in current-match [:data :view])]
                    [view current-match])
                  [not-found-page])}]))

(defn init []
  (theme/initialize-theme)
  (initial-auth-check)
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (state/set-current-route m))
   {:use-fragment false})

  (rdom/render (rdom/create-root (js/document.getElementById "app")) [app])

  (fetch-current-user-details))

(defn reload []
  (println "Reloading frontend...")
  (init))
