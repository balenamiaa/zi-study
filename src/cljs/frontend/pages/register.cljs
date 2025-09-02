(ns frontend.pages.register
  (:require ["lucide-react" :refer [UserPlus Mail Lock User]]
            [frontend.state.auth.handlers :as auth-h]
            [re-frame.core :as rf]
            [uix.core :as uix :refer [$ defui]]))

(defui register-page [_match]
  (let [[name set-name!] (uix/use-state "")
        [email set-email!] (uix/use-state "")
        [password set-password!] (uix/use-state "")
        [remember? set-remember!] (uix/use-state true)]
    ($ :div {:class "max-w-md mx-auto space-y-6"}
       ($ :h1 {:class "text-3xl font-bold text-center"} "Create account")
       ($ :div {:class "card bg-base-200 shadow"}
          ($ :div {:class "card-body space-y-4"}
             ($ :label {:class "input input-bordered flex items-center gap-2"}
                ($ User {:size 18})
                ($ :input {:type "text" :placeholder "Name" :class "grow"
                           :value name :on-change #(set-name! (.. % -target -value))}))
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
                         :on-click #(rf/dispatch [::auth-h/register {:name name :email email :password password :remember? remember?}
                                                  [:navigate/home]])}
                ($ UserPlus {:size 18})
                ($ :span {:class "ml-2"} "Create account")))))))

