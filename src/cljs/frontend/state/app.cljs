(ns frontend.state.app
  (:require [frontend.state.handlers :as state-h]
            [frontend.state.auth.handlers :as auth-h]
            [re-frame.core :as rf]))

(defn init! []
  ;; theme first (sets DOM + persists)
  (state-h/init-theme!)
  ;; bootstrap auth session
  (rf/dispatch [::auth-h/get-me]))

