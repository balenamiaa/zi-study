(ns zi-study.frontend.core
  (:require [reagent.dom.client :as rdom]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.controllers :as rfc]
            [reitit.coercion.spec :as rss]
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
            [zi-study.frontend.utilities.auth-guard :refer [auth-controller-start]]
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
     :view question-sets-page
     :controllers [{:identity (fn [match] match)
                    :start auth-controller-start}]
     :protected? true
     :auth-opts {:redirect-to ::login
                 :message "Please log in to view the question sets"}}]

   ["/question-sets/:set-id"
    {:name ::set-page
     :view set-page
     :controllers [{:identity (fn [match] match)
                    :start auth-controller-start}]
     :protected? true
     :auth-opts {:redirect-to ::login
                 :message "Please log in to view the question set"}
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
  (let [token (auth-core/get-token)]
    (if token
      (state/set-provisionally-authenticated token)
      (state/set-unauthenticated))))


(defn app []
  (let [current-match @(state/get-current-route)
        current-auth-state @(state/get-auth-state) ; Get current auth state
        current-route-name (get-in current-match [:data :name])]

    [main-layout
     {:current-route current-route-name
      :children
      (if current-match
        (if (and (get-in current-match [:data :protected?]) ; Is it a protected route?
                 (not (:authenticated? current-auth-state))) ; And user is not authenticated?
          nil
          (let [view (get-in current-match [:data :view])]
            [view current-match]))
        [not-found-page])}]))

(defn init []
  (theme/initialize-theme)
  (initial-auth-check)

  (let [app-state-current-route-atom (state/get-current-route)]
    (rfe/start!
     (rf/router routes {:data {:coercion rss/coercion
                               ;; Define global controllers here if needed
                               ;; :controllers [...]
                               }})
     (fn on-navigate [new-match]
       (let [old-match @app-state-current-route-atom
             active-controllers (rfc/apply-controllers
                                 (get old-match :active-controllers)
                                 new-match)]
         (if new-match
           (state/set-current-route (assoc new-match :active-controllers active-controllers))
           (state/set-current-route nil))))
     {:use-fragment false}))

  (rdom/render (rdom/create-root (js/document.getElementById "app")) [app])


  (let [auth-state-atom (state/get-auth-state)
        current-route-atom (state/get-current-route)]
    (add-watch auth-state-atom :global-auth-change-watcher
               (fn [_key _atom old-auth-state new-auth-state]
                 (let [current-match @current-route-atom]
                   (when (and current-match
                              (get-in current-match [:data :protected?]) ; On a protected route
                              (false? (:authenticated? new-auth-state)) ; Became unauthenticated
                              (not (false? (:authenticated? old-auth-state))) ; Was previously authenticated/pending
                              (not (:loading-current-user new-auth-state))) ; Not just loading
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
