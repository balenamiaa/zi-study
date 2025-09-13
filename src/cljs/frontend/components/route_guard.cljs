(ns frontend.components.route-guard
  (:require [frontend.routes :as routes]
            [frontend.state.auth :as auth]
            [frontend.uix.hooks :refer [use-subscribe]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(defui route-guard [{:keys [current-match]}]
  (let [route-data (get current-match :data {})
        auth* (use-subscribe [::auth/auth])
        status (:status auth*)
        unauth? (= status :unauthenticated)]
    (uix/use-effect
     (fn []
       (when (and current-match (:protected? route-data) unauth?)
         (let [name (:name route-data)
               params (:path-params current-match)
               query (:query-params current-match)]
           (rfe/replace-state routes/sym-login-route {:redirect {:name name :params params :query query}}))))
     [unauth? current-match status route-data])
    ($ :<>)))
