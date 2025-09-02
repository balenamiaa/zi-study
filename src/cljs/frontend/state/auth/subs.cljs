(ns frontend.state.auth.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::auth
 (fn [db _]
   (:auth db)))

(rf/reg-sub
 ::authenticated?
 :<- [::auth]
 (fn [auth _]
   (= (:status auth) :authenticated)))

(rf/reg-sub
 ::current-user
 :<- [::auth]
 (fn [auth _]
   (:user auth)))

