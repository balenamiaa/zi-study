(ns frontend.state.app
  (:require [frontend.state.auth :as auth]
            [frontend.state.ui :as state-ui]
            [frontend.state.security :as security]
            [re-frame.core :as rf]))

(defn init! []
  ;; theme first (sets DOM + persists)
  (state-ui/init-theme!)
  ;; bootstrap auth session
  (rf/dispatch [::auth/get-me]))
