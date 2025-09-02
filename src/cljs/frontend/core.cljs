(ns frontend.core
  (:require  [frontend.layouts.main-layout :refer [main-layout]]
             [frontend.pages.about :refer [about-page]]
             [frontend.pages.home :refer [home-page]]
             [frontend.pages.not-found :refer [not-found-page]]
             [frontend.pages.todos :refer [todos-page]]
             [frontend.pages.login :refer [login-page]]
             [frontend.pages.register :refer [register-page]]
             [frontend.routes :as routes :refer [mk-routes]]
             [frontend.components.route-guard :refer [route-guard]]
             [frontend.state.handlers :as state-h]
             [frontend.state.subs :as state-subs]
             [frontend.state.auth.handlers :as auth-h]
             [frontend.state.auth.subs :as auth-subs]
             [frontend.state.app :as app]
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
        authenticated? (use-subscribe [::auth-subs/authenticated?])
        route-data (get current-match :data {})
        current-route-name (:name route-data)
        layout-component (get route-data :layout main-layout)
        view-component (get route-data :view)]

    ($ layout-component
       {:current-route current-route-name}
       ($ route-guard {:current-match current-match :authenticated? authenticated?})
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
  (app/init!)

  (let [routes (mk-routes {:main-layout main-layout
                           :home-page home-page
                           :todos-page todos-page
                           :about-page about-page
                           :login-page login-page
                           :register-page register-page
                           :not-found-page not-found-page})
        _app-state-current-route-atom nil]
    (rfe/start!
     (reitit/router routes {:data {:coercion rss/coercion}})
     (fn on-navigate [new-match]
       (let [old-match (:current-route @rfdb/app-db)
             controllers (rfc/apply-controllers (:controllers old-match) new-match)]
         (rf/dispatch [::state-h/set-current-route (assoc new-match :controllers controllers)])))
     {:use-fragment false}))

  (render))
