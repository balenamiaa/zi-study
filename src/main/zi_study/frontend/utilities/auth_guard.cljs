(ns zi-study.frontend.utilities.auth-guard
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.state :as state]))

(defn with-auth
  "Auth guard component that either:
   1. Shows the given content if the user is authenticated
   2. Redirects to login with a flash message if not authenticated
  
   Options:
   - content: (required) Hiccup or component to render when authenticated
   - redirect-to: Route name to redirect to if not authenticated (default :login)
   - message: Custom message to show in the flash (default 'You need to be logged in to access this page')
   - message-variant: Flash message variant (default :filled)
   - message-position: Position of flash message (default :top-right)
   - redirect-params: Optional params for redirect route
   - loading-component: Component to show while auth is loading (default empty div)"
  [{:keys [content redirect-to message message-variant message-position redirect-params loading-component]
    :or {redirect-to :zi-study.frontend.core/login
         message "You need to be logged in to access this page"
         message-variant :filled
         message-position :top-right
         loading-component [:<>]}}]

  (let [auth-state (state/get-auth-state)
        checked? (r/atom false)]

    (r/create-class
     {:display-name "AuthGuard"

      :component-did-mount
      (fn [_]
        ;; Set up a watch on the auth state
        (add-watch auth-state ::auth-check
                   (fn [_ _ _ new-state]
                     ;; Only proceed when loading is complete
                     (when (and (not (:loading? new-state))
                                (not @checked?))
                       (reset! checked? true)
                       (when-not (:authenticated? new-state)
                         ;; Show flash message
                         (state/flash-error message
                                            :variant message-variant
                                            :position message-position
                                            :auto-hide 8000)
                         ;; Redirect to login
                         (rfe/push-state redirect-to redirect-params))))))

      :component-will-unmount
      (fn [_]
        ;; Clean up watcher
        (remove-watch auth-state ::auth-check))

      :reagent-render
      (fn [_]
        (let [current-auth @auth-state]
          (cond
            ;; If authenticated, render content
            (:authenticated? current-auth)
            content

            ;; If loading, render loading component
            (:loading? current-auth)
            loading-component

            ;; Not loading and not authenticated, render nothing (will redirect)
            :else
            [:<>])))})))

(defn protected-route
  "A wrapper component for routes that require authentication.
   Use this in your routes to protect pages that require login."
  [props]
  [with-auth props])
