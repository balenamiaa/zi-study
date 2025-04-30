(ns zi-study.frontend.core
  (:require [reagent.dom.client :as rdom]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [goog.object :as gobj]
            [clojure.string :as str]
            [zi-study.frontend.pages.home :refer [home-page]]
            [zi-study.frontend.pages.counter :refer [counter-page]]
            [zi-study.frontend.pages.items :refer [demo-item-page]]
            [zi-study.frontend.pages.components :refer [components-page]]
            [zi-study.frontend.pages.not-found :refer [not-found-page]]
            [zi-study.frontend.pages.login :refer [login-page]]
            [zi-study.frontend.pages.register :refer [register-page]]
            [zi-study.frontend.layouts.main-layout :refer [main-layout]]
            [zi-study.frontend.state :refer [app-state router-match]]))



(def routes
  [["/"
    {:name ::home
     :view (home-page)}]

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
                  :query map?}}]])


(defn app []
  (let [current-route (:name (:data @router-match))
        auth-state (select-keys @app-state [:auth/loading?
                                            :auth/authenticated?
                                            :auth/current-user])]
    [main-layout
     {:current-route current-route
      :auth-state auth-state
      :children (if @router-match
                  (let [view (:view (:data @router-match))]
                    [view @router-match])
                  [not-found-page])}]))

(defn check-auth-status []
  (let [token (.getItem js/localStorage "auth-token")]
    (if (not (str/blank? token))
      (do
        (swap! app-state assoc :auth/token token :auth/loading? true)
        (-> (js/fetch "/api/auth/me"
                      (clj->js {:headers {"Authorization" (str "Bearer " token)}}))
            (.then (fn [response]
                     (if (.-ok response)
                       (.json response)
                       (js/Promise.reject "Invalid token"))))
            (.then (fn [data]
                     (when-let [user (gobj/get data "user")]
                       (swap! app-state assoc
                              :auth/authenticated? true
                              :auth/current-user (js->clj user :keywordize-keys true)
                              :auth/loading? false))))
            (.catch (fn [_err]
                      (.removeItem js/localStorage "auth-token")
                      (swap! app-state assoc
                             :auth/authenticated? false
                             :auth/token nil
                             :auth/current-user nil
                             :auth/loading? false)))))
      (swap! app-state assoc :auth/loading? false :auth/authenticated? false :auth/token nil :auth/current-user nil))))

(defn init []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (reset! router-match m))
   {:use-fragment false})
  (rdom/render (rdom/create-root (js/document.getElementById "app")) [app])
  (check-auth-status))

(defn reload []
  (println "Reloading frontend...")
  (init))
