(ns frontend.core
  (:require  [frontend.layouts.main-layout :refer [main-layout]]
             [frontend.pages.about :refer [about-page]]
             [frontend.pages.home :refer [home-page]]
             [frontend.pages.not-found :refer [not-found-page]]
             [frontend.pages.todos :refer [todos-page]]
             [frontend.routes :refer [mk-routes]]
             [frontend.state.handlers :as state-h]
             [frontend.state.subs :as state-subs]
             [frontend.uix.hooks :refer [use-subscribe]]
             [frontend.utilities.theme :as theme]
             [re-frame.core :as rf]
             [re-frame.db :as rfdb]
             [reitit.coercion.spec :as rss]
             [reitit.frontend :as reitit]
             [reitit.frontend.controllers :as rfc]
             [reitit.frontend.easy :as rfe]
             [uix.core :as uix :refer [$ defui]]
             [uix.dom]))

(defui app []
  (let [current-match (use-subscribe [::state-subs/current-route])
        route-data (get current-match :data {})
        current-route-name (:name route-data)
        layout-component (get route-data :layout main-layout)
        view-component (get route-data :view)]

    ($ layout-component
       {:current-route current-route-name}
       (if current-match
         (when view-component
           ($ view-component current-match))
         ($ not-found-page)))))

(defonce root
  (when-let [el (js/document.getElementById "app")]
    (uix.dom/create-root el)))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (state-h/init-theme!)

  (let [routes (mk-routes {:main-layout main-layout
                           :home-page home-page
                           :todos-page todos-page
                           :about-page about-page
                           :not-found-page not-found-page})
        ;; Avoid re-frame warning: subscribe used outside reactive context.
        ;; Read old route directly from app-db inside router callback.
        _app-state-current-route-atom nil]
    (rfe/start!
     (reitit/router routes {:data {:coercion rss/coercion}})
     (fn on-navigate [new-match]
       (let [old-match (:current-route @rfdb/app-db)
             controllers (rfc/apply-controllers (:controllers old-match) new-match)]
         (rf/dispatch [::state-h/set-current-route (assoc new-match :controllers controllers)])))
     {:use-fragment false}))

  (render))
