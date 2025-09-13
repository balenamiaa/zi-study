(ns frontend.state.nav
  (:require
   [frontend.routes :as routes]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]))

(rf/reg-event-fx
 ::redirect
 (fn [_ [_ {:keys [name params query]}]]
   (rfe/push-state name params query)
   {}))

(rf/reg-event-fx
 ::home
 (fn [_ _]
   (rfe/push-state routes/sym-home-route)
   {}))

