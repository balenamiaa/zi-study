(ns zi-study.frontend.state
  (:require [reagent.core :as r]))

;; Core application state
(defonce app-state
  (r/atom {:auth {:loading? true
                  :token nil
                  :authenticated? false
                  :current-user nil}
           :ui {:theme :system
                :sidebar-open? false}
           :router {:current-match nil}}))

;; Selectors
(defn get-auth-state []
  (:auth @app-state))

(defn get-ui-state []
  (:ui @app-state))

(defn get-current-route []
  (get-in @app-state [:router :current-match]))

;; Auth state updaters
(defn set-auth-loading [loading?]
  (swap! app-state assoc-in [:auth :loading?] loading?))

(defn set-authenticated [authenticated? token user]
  (swap! app-state assoc-in [:auth]
         {:loading? false
          :authenticated? authenticated?
          :token token
          :current-user user}))

(defn reset-auth-state!
  "Resets the authentication part of the app state to initial values."
  []
  (swap! app-state assoc-in [:auth]
         {:loading? false
          :authenticated? false
          :token nil
          :current-user nil}))

;; Router state updaters
(defn set-current-route [match]
  (swap! app-state assoc-in [:router :current-match] match))

;; UI state updaters
(defn set-theme [theme]
  (swap! app-state assoc-in [:ui :theme] theme))

(defn toggle-sidebar []
  (swap! app-state update-in [:ui :sidebar-open?] not))