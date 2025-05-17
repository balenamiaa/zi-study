(ns zi-study.frontend.core
  (:require [reagent.dom.client :as rdom]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.controllers :as rfc]
            [reitit.coercion.spec :as rss]
            [zi-study.frontend.pages.home :refer [home-page]]
            [zi-study.frontend.pages.not-found :refer [not-found-page]]
            [zi-study.frontend.pages.login :refer [login-page]]
            [zi-study.frontend.pages.register :refer [register-page]]
            [zi-study.frontend.pages.question-sets :refer [question-sets-page]]
            [zi-study.frontend.pages.advanced-search :refer [advanced-search-page]]
            [zi-study.frontend.pages.set-page :refer [set-page]]
            [zi-study.frontend.pages.my-folders :refer [my-folders-page]]
            [zi-study.frontend.pages.public-folders :refer [public-folders-page]]
            [zi-study.frontend.pages.folder-details :refer [folder-details-page]]
            [zi-study.frontend.routes :refer [mk-routes]]
            [zi-study.frontend.layouts.main-layout :refer [main-layout]]
            [zi-study.frontend.layouts.active-learning-layout :refer [active-learning-layout]]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.auth :as auth]
            [zi-study.frontend.utilities.auth-core :as auth-core]
            [zi-study.frontend.utilities.theme :as theme]))



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
  (let [token (auth-core/get-token)]
    (if token
      (state/set-provisionally-authenticated token)
      (state/set-unauthenticated))))

(defn app []
  (let [current-match @(state/get-current-route)
        current-auth-state @(state/get-auth-state)
        route-data (get current-match :data {})
        current-route-name (:name route-data)
        layout-component (get route-data :layout main-layout)
        view-component (get route-data :view)]

    [layout-component
     {:current-route current-route-name
      :active-learning-page (when (= layout-component active-learning-layout)
                              (get route-data :active-learning-page))
      :children
      (if current-match
        (if (and (:protected? route-data)
                 (not (:authenticated? current-auth-state)))
          nil
          (when view-component [view-component current-match]))
        [not-found-page])}]))

(defn init []
  (theme/initialize-theme)
  (initial-auth-check)

  (let [routes (mk-routes {:main-layout main-layout
                           :active-learning-layout active-learning-layout
                           :home-page home-page
                           :login-page login-page
                           :register-page register-page
                           :question-sets-page question-sets-page
                           :set-page set-page
                           :advanced-search-page advanced-search-page
                           :my-folders-page my-folders-page
                           :folder-details-page folder-details-page
                           :public-folders-page public-folders-page})
        app-state-current-route-atom (state/get-current-route)]
    (rfe/start!
     (rf/router routes {:data {:coercion rss/coercion
                               :controllers []}})
     (fn on-navigate [new-match]
       (let [old-match @app-state-current-route-atom
             active-controllers (rfc/apply-controllers
                                 (get old-match :active-controllers)
                                 new-match)
             final-match (if new-match (assoc new-match :active-controllers active-controllers) nil)]
         (state/set-current-route final-match)))
     {:use-fragment false}))

  (rdom/render (rdom/create-root (js/document.getElementById "app")) [app])

  (let [auth-state-atom (state/get-auth-state)
        current-route-atom (state/get-current-route)]
    (add-watch auth-state-atom :global-auth-change-watcher
               (fn [_key _atom old-auth-state new-auth-state]
                 (let [current-match @current-route-atom]
                   (when (and current-match
                              (get-in current-match [:data :protected?])
                              (false? (:authenticated? new-auth-state))
                              (not (false? (:authenticated? old-auth-state)))
                              (not (:loading-current-user? new-auth-state)))
                     (let [auth-opts (get-in current-match [:data :auth-opts] {})
                           redirect-to (or (:redirect-to auth-opts) ::login)
                           message (or (get auth-opts :message-on-expiry)
                                       (get auth-opts :message)
                                       "Your session has expired. Please log in again.")
                           message-variant (or (:message-variant auth-opts) :filled)
                           message-position (or (:message-position auth-opts) :top-right)
                           redirect-params (or (:redirect-params auth-opts) {})]
                       (state/flash-error message
                                          :variant message-variant
                                          :position message-position
                                          :auto-hide 8000)
                       (rfe/push-state redirect-to redirect-params)))))))
  (fetch-current-user-details))

(defn reload []
  (println "Reloading frontend...")
  (init))
