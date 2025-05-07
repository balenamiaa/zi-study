(ns zi-study.frontend.core
  (:require [reagent.dom.client :as rdom]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [zi-study.frontend.pages.home :refer [home-page]]
            [zi-study.frontend.pages.counter :refer [counter-page]]
            [zi-study.frontend.pages.items :refer [demo-item-page]]
            [zi-study.frontend.pages.components :refer [components-page]]
            [zi-study.frontend.pages.not-found :refer [not-found-page]]
            [zi-study.frontend.pages.login :refer [login-page]]
            [zi-study.frontend.pages.register :refer [register-page]]
            [zi-study.frontend.pages.question-sets :refer [question-sets-page]]
            [zi-study.frontend.pages.set-page :refer [set-page]]
            [zi-study.frontend.pages.bookmarks :refer [bookmarks-page]]
            [zi-study.frontend.layouts.main-layout :refer [main-layout]]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.auth :as auth]
            [zi-study.frontend.utilities.auth-core :as auth-core]
            [zi-study.frontend.utilities.theme :as theme]))



(def routes
  [["/"
    {:name ::home
     :view home-page}]

   ["/counter"
    {:name ::counter
     :view counter-page}]

   ["/components"
    {:name ::components
     :view components-page}]

   ["/login"
    {:name ::login
     :view login-page}]

   ["/register"
    {:name ::register
     :view register-page}]

   ["/item/:id"
    {:name ::item
     :view demo-item-page
     :parameters {:path {:id int?}
                  :query map?}}]

   ["/question-sets"
    {:name ::question-sets
     :view question-sets-page}]

   ["/question-sets/:set-id"
    {:name ::set-page
     :view set-page
     :parameters {:path {:set-id int?}}}]

   ["/bookmarks"
    {:name ::bookmarks
     :view bookmarks-page}]])


(defn app []
  (let [current-match (state/get-current-route)
        current-route (get-in current-match [:data :name])]
    [main-layout
     {:current-route current-route
      :children (if current-match
                  (let [view (get-in current-match [:data :view])]
                    [view current-match])
                  [not-found-page])}]))

(defn check-auth-status []
  (state/set-auth-loading true)
  (auth/get-current-user
   (fn [result]
     (if (:success result)
       (state/set-authenticated true (auth-core/get-token) (:user result))
       (state/set-authenticated false nil nil)))))

(defn init []
  (theme/initialize-theme)

  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (state/set-current-route m))
   {:use-fragment false})

  (rdom/render (rdom/create-root (js/document.getElementById "app")) [app])

  (check-auth-status))

(defn reload []
  (println "Reloading frontend...")
  (init))
