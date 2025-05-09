(ns zi-study.frontend.utilities.auth-guard
  (:require [reitit.frontend.easy :as rfe]
            [zi-study.frontend.state :as state]))


(defn auth-controller-start
  "Controller :start function.
   Receives the controller's identity (which we'll set to be the current route match).
   Checks authentication and redirects if necessary."
  [current-route-match]
  (let [auth-opts (get-in current-route-match [:data :auth-opts] {}) ; Get from route data
        redirect-to (or (:redirect-to auth-opts) ::login)
        message (or (:message auth-opts) "You need to be logged in to access this page.")
        message-variant (or (:message-variant auth-opts) :filled)
        message-position (or (:message-position auth-opts) :top-right)
        redirect-params (or (:redirect-params auth-opts) {})
        current-auth-state @(state/get-auth-state)] ; Dereference the auth state atom

    (when (not (:authenticated? current-auth-state))
      (state/flash-error message
                         :variant message-variant
                         :position message-position
                         :auto-hide 8000)
      (js/setTimeout #(rfe/push-state redirect-to redirect-params) 0))))