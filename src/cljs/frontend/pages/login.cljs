(ns frontend.pages.login
  (:require ["lucide-react" :refer [LogIn Mail Lock KeyRound]]
            [frontend.routes :as routes]
            [frontend.state.auth.handlers :as auth-h]
            [frontend.state.auth.subs :as auth-subs]
            [frontend.uix.hooks :refer [use-subscribe]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(defui login-page [{:keys [query-params]}]
  (let [[email set-email!] (uix/use-state "")
        [password set-password!] (uix/use-state "")
        [remember? set-remember!] (uix/use-state true)
        redirect-to (get query-params :redirect)
        on-success (if redirect-to [:navigate/redirect redirect-to] [:navigate/home])]
    ($ :div {:class "max-w-md mx-auto space-y-6"}
       ($ :h1 {:class "text-3xl font-bold text-center"} "Sign in")
       ($ :div {:class "card bg-base-200 shadow"}
          ($ :div {:class "card-body space-y-4"}
             ($ :label {:class "input input-bordered flex items-center gap-2"}
                ($ Mail {:size 18})
                ($ :input {:type "email" :placeholder "Email" :class "grow"
                           :value email :on-change #(set-email! (.. % -target -value))}))
             ($ :label {:class "input input-bordered flex items-center gap-2"}
                ($ Lock {:size 18})
                ($ :input {:type "password" :placeholder "Password" :class "grow"
                           :value password :on-change #(set-password! (.. % -target -value))}))
             ($ :label {:class "label cursor-pointer justify-start gap-2"}
                ($ :input {:type "checkbox" :checked remember?
                           :class "checkbox checkbox-primary"
                           :on-change #(set-remember! (.. % -target -checked))})
                ($ :span {:class "label-text"} "Remember me for 30 days"))
             ($ :button {:class "btn btn-primary w-full"
                         :on-click #(rf/dispatch [::auth-h/login {:email email :password password :remember? remember?} on-success])}
                ($ LogIn {:size 18})
                ($ :span {:class "ml-2"} "Sign in"))
             ($ :div {:class "divider"} "or")
             ($ :a {:href "/api/auth/google/start" :class "btn btn-outline w-full"}
                ($ KeyRound {:size 18})
                ($ :span {:class "ml-2"} "Continue with Google")))))))

;; Navigation helpers as events
(rf/reg-event-fx
 :navigate/redirect
 (fn [_ [_ {:keys [name params query]}]]
   (rfe/push-state name params query)
   {}))

(rf/reg-event-fx
 :navigate/home
 (fn [_ _]
   (rfe/push-state routes/sym-home-route)
   {}))
