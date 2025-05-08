(ns zi-study.frontend.utilities.auth-guard
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.state :as state]))

(defn with-auth
  "Auth guard component that either:
   1. Shows the given content if the user is authenticated (even provisionally).
   2. Redirects to login with a flash message if not authenticated after initial checks.
  
   The component expects that the global auth state (`:loading-current-user?`)
   will reflect the process of fetching full user details if needed, and the
   guarded `content` itself should handle displaying any specific loading indicators
   for that phase.

   Options:
   - content: (required) Hiccup or component to render when authenticated
   - redirect-to: Route name to redirect to if not authenticated (default :login)
   - message: Custom message to show in the flash (default 'You need to be logged in to access this page')
   - message-variant: Flash message variant (default :filled)
   - message-position: Position of flash message (default :top-right)
   - redirect-params: Optional params for redirect route"
  [{:keys [content redirect-to message message-variant message-position redirect-params]
    :or {redirect-to :zi-study.frontend.core/login
         message "You need to be logged in to access this page"
         message-variant :filled
         message-position :top-right}}]

  (let [rx-auth-state (state/get-auth-state)]

    (r/create-class
     {:display-name "AuthGuard"

      :component-did-mount
      (fn [_this]
        (let [current-auth @rx-auth-state]
          (when (not (:authenticated? current-auth))
            (state/flash-error message
                               :variant message-variant
                               :position message-position
                               :auto-hide 8000)
            (rfe/push-state redirect-to redirect-params))))

      :component-did-update
      (fn [_this _old-argv]
        (let [current-auth @rx-auth-state]
          ;; If state updates to unauthenticated (after loading), etc...
          (when (not (:authenticated? current-auth))
            (state/flash-error message
                               :variant message-variant
                               :position message-position
                               :auto-hide 8000)
            (rfe/push-state redirect-to redirect-params))))

      :reagent-render
      (fn [_this]
        (let [current-auth @rx-auth-state]
          (cond
            ;; If authenticated (even provisionally), show content.
            ;; The content itself should handle its own loading state for full user details.
            (:authenticated? current-auth)
            content

            :else
            nil)))})))

(defn protected-route
  "A wrapper component for routes that require authentication.
   Use this in your routes to protect pages that require login."
  [props]
  [with-auth props])
