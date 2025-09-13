(ns frontend.state.user
  (:require [frontend.state.auth :as auth]
            [frontend.state.http-fx :as http]
            [frontend.state.ui-fx :as ui-fx]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 ::update-profile
 (fn [_ [_ {:keys [name]}]]
   {::http/fetch {:method :post :url "/api/user/profile"
                  :body {:name name}
                  :on-success [::update-profile-success]
                  :on-failure [::update-profile-failure]}}))

(rf/reg-event-fx
 ::update-profile-success
 (fn [_ [_ _resp]]
   {:fx [[:dispatch [::auth/get-me]]]
    ::ui-fx/toast {:text "Profile updated" :variant :success}}))

(rf/reg-event-fx
 ::update-profile-failure
 (fn [_ [_ _resp]]
   {::ui-fx/toast {:text "Could not update profile" :variant :error}}))

(rf/reg-event-fx
 ::change-password
 (fn [_ [_ {:keys [current new confirm]} on-success]]
   (let [body (cond-> {:new new}
                (seq (str current)) (assoc :current current)
                (some? confirm) (assoc :confirm confirm))]
     {::http/fetch {:method :post :url "/api/user/password"
                    :body body
                    :on-success [::change-password-success on-success]
                    :on-failure [::change-password-failure]}})))

(rf/reg-event-fx
 ::change-password-success
 (fn [_ [_ on-success _resp]]
   (merge {::ui-fx/toast {:text "Password updated" :variant :success}}
          (when on-success {:fx [[:dispatch on-success]]}))))

(rf/reg-event-fx
 ::change-password-failure
 (fn [_ [_ _resp]]
   {::ui-fx/toast {:text "Password change failed" :variant :error}}))

(rf/reg-event-fx
 ::avatar-uploaded
 (fn [_ [_ _resp]]
   {:fx [[:dispatch [::auth/get-me]]]
    ::ui-fx/toast {:text "Avatar updated" :variant :success}}))

(rf/reg-event-fx
 ::remove-avatar
 (fn [_ _]
   {::http/fetch {:method :delete :url "/api/user/avatar"
                  :on-success [::remove-avatar-success]
                  :on-failure [::remove-avatar-failure]}}))

(rf/reg-event-fx
 ::remove-avatar-success
 (fn [_ _]
   {:fx [[:dispatch [::auth/get-me]]]
    ::ui-fx/toast {:text "Avatar removed" :variant :success}}))

(rf/reg-event-fx
 ::remove-avatar-failure
 (fn [_ _]
   {::ui-fx/toast {:text "Could not remove avatar" :variant :error}}))
