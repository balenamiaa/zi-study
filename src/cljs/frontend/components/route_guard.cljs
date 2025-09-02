(ns frontend.components.route-guard
  (:require [frontend.routes :as routes]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(defui route-guard [{:keys [current-match authenticated?]}]
  (let [route-data (get current-match :data {})]
    (uix/use-effect
     (fn []
       (when (and current-match (:protected? route-data) (false? authenticated?))
         (let [name (:name route-data)
               params (:path-params current-match)
               query (:query-params current-match)]
           (rfe/replace-state routes/sym-login-route {:redirect {:name name :params params :query query}}))))
     [current-match authenticated? route-data])
    ;; renders nothing
    ($ :<>)))

