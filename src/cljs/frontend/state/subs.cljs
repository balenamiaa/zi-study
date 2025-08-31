(ns frontend.state.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::current-route
 (fn [db _]
   (:current-route db)))

(rf/reg-sub
 ::ui-state
 (fn [db _]
   (:ui db)))

