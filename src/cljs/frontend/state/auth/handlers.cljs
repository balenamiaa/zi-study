(ns frontend.state.auth.handlers
  (:require [frontend.state.http-fx :as http]
            [re-frame.core :as rf]))

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
 ::get-me
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :status] :loading)
    :fx [[:dispatch [:http/init [:auth :me]]]
         [::http/fetch {:method :get :url "/api/auth/me"
                        :on-success [::get-me-success]
                        :on-failure [::get-me-failure]}]]}))

(rf/reg-event-db
 ::get-me-success
 (fn [db [_ user]]
   (set-authenticated db user)))

(rf/reg-event-db
 ::get-me-failure
 (fn [db _]
   (set-unauthenticated db)))

(rf/reg-event-fx
 ::login
 (fn [_ [_ {:keys [email password remember?]} on-success]]
   {::http/fetch {:method :post :url "/api/auth/login" :request-content-type :json
                  :body {:email email :password password :remember? remember?}
                  :on-success [::login-success on-success]
                  :on-failure [::login-failure]}}))

(rf/reg-event-fx
 ::login-success
 (fn [{:keys [db]} [_ on-success user]]
   (let [db' (set-authenticated db user)]
     (merge {:db db'}
            (when on-success {:fx [[:dispatch on-success]]})))))

(rf/reg-event-db
 ::login-failure
 (fn [db _]
   (assoc-in db [:auth :error] :invalid-credentials)))

(rf/reg-event-fx
 ::logout
 (fn [_ _]
   {::http/fetch {:method :post :url "/api/auth/logout"
                  :on-success [::logout-success]}}))

(rf/reg-event-db
 ::logout-success
 (fn [db _]
   (set-unauthenticated db)))

(rf/reg-event-fx
 ::register
 (fn [_ [_ {:keys [email password name remember?]} on-success]]
   {::http/fetch {:method :post :url "/api/auth/register" :request-content-type :json
                  :body {:email email :password password :name name :remember? remember?}
                  :on-success [::login-success on-success]
                  :on-failure [::register-failure]}}))

(rf/reg-event-fx
 ::upload-avatar
 (fn [_ [_ file on-success]]
   {::http/upload {:url "/api/user/avatar"
                   :file file
                   :on-success on-success
                   :on-failure [::register-failure]}}))

(rf/reg-event-db
 ::register-failure
 (fn [db _]
   (assoc-in db [:auth :error] :register-failed)))
