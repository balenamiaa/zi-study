(ns frontend.state.auth
  (:require [frontend.state.http-fx :as http]
            [frontend.state.security :as security]
            [frontend.state.ui-fx :as ui-fx]
            [re-frame.core :as rf]))

;; Subs (combined)
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

;; Helpers
(defn- set-authenticated [db user]
  (-> db
      (assoc-in [:auth :status] :authenticated)
      (assoc-in [:auth :user] user)))

(defn- set-unauthenticated [db]
  (-> db
      (assoc-in [:auth :status] :unauthenticated)
      (assoc-in [:auth :user] nil)))

;; Events
(rf/reg-event-fx
 ::login
 (fn [_ [_ {:keys [email password remember?]} on-success]]
   {::http/fetch {:method :post :url "/api/auth/login"
                  :body {:email email :password password :remember? remember?}
                  :on-success [::login-success on-success]
                  :on-failure [::login-failure]}}))

(rf/reg-event-fx
 ::login-success
 (fn [{:keys [db]} [_ on-success user]]
   (let [db' (set-authenticated db user)]
     (merge {:db db'
             ::start-heartbeat {:interval-ms (* 60 1000 9)}
             :fx [[:dispatch [::security/fetch-csrf]]]}
            (when on-success {:fx [[:dispatch on-success]]})))))

(rf/reg-event-fx
 ::login-failure
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :error] :invalid-credentials)
    ::ui-fx/toast {:text "Invalid email or password" :variant :error}}))

(rf/reg-event-fx
 ::logout
 (fn [_ _]
   {::http/fetch {:method :post :url "/api/auth/logout"
                  :on-success [::logout-success]
                  :on-failure [::logout-failure]}}))

(rf/reg-event-fx
 ::logout-success
 (fn [{:keys [db]} _]
   {:db (set-unauthenticated db)
    ::stop-heartbeat {}
    :fx [[:dispatch [::security/fetch-csrf]]]}))

(rf/reg-event-fx
 ::logout-failure
 (fn [{:keys [db]} [_ resp]]
   {:db db
    ::ui-fx/toast {:text "Logout failed" :variant :error}}))

(rf/reg-event-fx
 ::register
 (fn [_ [_ {:keys [email password name]} on-success]]
   {::http/fetch {:method :post :url "/api/auth/register"
                  :body {:email email :password password :name name}
                  :on-success [::login-success on-success]
                  :on-failure [::register-failure]}}))

(rf/reg-event-fx
 ::upload-avatar
 (fn [_ [_ file on-success]]
   {::http/upload {:url "/api/user/avatar"
                   :file file
                   :on-success on-success
                   :on-failure [::upload-avatar-failure]}}))

(rf/reg-event-fx
 ::upload-avatar-failure
 (fn [_ [_ resp]]
   {::ui-fx/toast {:text "Avatar upload failed" :variant :error}}))

(rf/reg-event-fx
 ::register-failure
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :error] :register-failed)
    ::ui-fx/toast {:text "Registration failed" :variant :error}}))

;; Session heartbeat: periodically hit /api/auth/me to refresh cookie TTL
(defonce ^:private heartbeat-id (atom nil))

(rf/reg-fx
 ::start-heartbeat
 (fn [{:keys [interval-ms]}]
   (when @heartbeat-id (js/clearInterval @heartbeat-id))
   (reset! heartbeat-id (js/setInterval (fn [] (rf/dispatch [::get-me])) interval-ms))))

(rf/reg-fx
 ::stop-heartbeat
 (fn [_]
   (when @heartbeat-id (js/clearInterval @heartbeat-id))
   (reset! heartbeat-id nil)))

(rf/reg-event-fx
 ::get-me
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :status] :loading)
    :fx [[:dispatch [:http/init [:auth :me]]]
         [::http/fetch {:method :get :url "/api/auth/me"
                        :on-success [::get-me-success]
                        :on-failure [::get-me-failure]}]]}))

(rf/reg-event-fx
 ::get-me-success
 (fn [{:keys [db]} [_ user]]
   {:db (set-authenticated db user)
    ::start-heartbeat {:interval-ms (* 60 1000 9)}
    :fx [[:dispatch [::security/fetch-csrf]]]}))

(rf/reg-event-fx
 ::get-me-failure
 (fn [{:keys [db]} _]
   {:db (set-unauthenticated db)
    ::stop-heartbeat {}}))
