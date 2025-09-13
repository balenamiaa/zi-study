 (ns frontend.state.security
  (:require [frontend.state.http-fx :as http]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 ::fetch-csrf
 (fn [_ _]
   {::http/fetch {:method :get :url "/api/auth/csrf"
                  :on-success [::fetch-csrf-success]}}))

(rf/reg-event-fx
 ::fetch-csrf-success
 (fn [_ [_ resp]]
   (let [token (or (get-in resp [:body :token])
                   (:token resp))]
     {:fx [[:dispatch [::http/set-csrf-token token]]]})))

;; Fetch CSRF then run a follow-up event (e.g., retry the original request)
(rf/reg-event-fx
 ::fetch-csrf-and
 (fn [_ [_ then-event]]
   {::http/fetch {:method :get :url "/api/auth/csrf"
                  :on-success [::fetch-csrf-success-and then-event]}}))

(rf/reg-event-fx
 ::fetch-csrf-success-and
 (fn [_ [_ then-event resp]]
   (let [token (or (get-in resp [:body :token])
                   (:token resp))]
     {:fx [[:dispatch [::http/set-csrf-token token]]
           (when then-event [:dispatch then-event])]})))
