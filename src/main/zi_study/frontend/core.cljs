(ns zi-study.frontend.core
  (:require [reagent.dom.client :as rdom]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [zi-study.frontend.pages.home :refer [home-page]]
            [zi-study.frontend.pages.counter :refer [counter-page]]
            [zi-study.frontend.pages.items :refer [demo-item-page]]
            [zi-study.frontend.layouts.main-layout :refer [main-layout]]
            [zi-study.frontend.state :refer [app-state router-match]]))



(def routes
  [["/"
    {:name ::home
     :view (home-page)}]

   ["/counter"
    {:name ::counter
     :view counter-page}]

   ["/item/:id"
    {:name ::item
     :view demo-item-page
     :parameters {:path {:id int?}
                  :query map?}}]])


(defn app []
  (let [current-route (:name (:data @router-match))]
    [main-layout
     {:current-route current-route
      :children (if @router-match
                  (let [view (:view (:data @router-match))]
                    [view @router-match])
                  [:div "Loading..."])}]))

(defn init []
  (println "Initializing frontend...")
  (let [root-styles (.. js/document -documentElement -style)]
    (.setProperty root-styles "--color-light-bg" "#F8F9FC")
    (.setProperty root-styles "--color-light-bg-paper" "#F1F3F9")
    (.setProperty root-styles "--color-light-card" "#FFFFFF"))

  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (reset! router-match m))
   {:use-fragment false})
  (rdom/render (rdom/create-root (js/document.getElementById "app")) [app]))

(defn reload []
  (println "Reloading frontend...")
  (init))
