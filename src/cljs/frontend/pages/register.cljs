(ns frontend.pages.register
  (:require ["lucide-react" :refer [UserPlus Mail Lock User Sparkles Shield]]
            [clojure.string :as str]
            [frontend.routes :as routes]
            [frontend.state.auth :as auth]
            [frontend.state.nav :as nav]
            [frontend.ui.button :refer [Button]]
            [frontend.ui.card :refer [Card]]
            [frontend.ui.field :refer [FieldWrapper TextInput PasswordInput]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(defui register-page [_match]
  (let [[name set-name!] (uix/use-state "")
        [email set-email!] (uix/use-state "")
        [password set-password!] (uix/use-state "")
        [show-pass? set-show-pass!] (uix/use-state false)
        email-valid? (boolean (re-find #".+@.+" email))
        name-valid? (>= (count (str/trim name)) 2)
        pass-valid? (>= (count password) 8)
        can-submit? (and email-valid? name-valid? pass-valid?)]
    ($ :div {:class "relative min-h-[80vh] grid place-items-center px-4"}
       ($ :div {:class "absolute inset-0 -z-10 pointer-events-none"}
          ($ :div {:class "absolute -top-24 -left-16 w-72 h-72 bg-primary/15 rounded-full blur-3xl"})
          ($ :div {:class "absolute -bottom-24 -right-16 w-72 h-72 bg-accent/20 rounded-full blur-3xl"}))

       ($ :div {:class "w-full max-w-md"}
          ($ :div {:class "text-center mb-6 space-y-2"}
             ($ :div {:class "inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-primary/10 text-primary shadow-md"}
                ($ Sparkles {:size 24}))
             ($ :h1 {:class "text-heading-2"} "Create your account")
             ($ :p {:class "text-body text-base-content/70"}
                "Join and get started in seconds."))

          ($ Card {:glass? true}
             ($ :div {:class "card-body space-y-5"}
                ($ FieldWrapper {:id "name" :label "Full name"
                                 :hint (when (and (not (empty? name)) (not name-valid?))
                                         "Name must be at least 2 characters.")}
                   ($ TextInput {:id "name" :full? true :start-icon User
                                 :placeholder "Jane Doe"
                                 :invalid? (and (not (empty? name)) (not name-valid?))
                                 :value name :on-change #(set-name! (.. % -target -value))}))

                ($ FieldWrapper {:id "email" :label "Email"
                                 :hint (when (and (not (empty? email)) (not email-valid?))
                                         "Enter a valid email.")}
                   ($ TextInput {:id "email" :full? true :start-icon Mail
                                 :type "email"
                                 :placeholder "you@domain.com"
                                 :invalid? (and (not (empty? email)) (not email-valid?))
                                 :value email :on-change #(set-email! (.. % -target -value))}))

                ($ FieldWrapper {:id "password" :label "Password"
                                 :hint "Use a strong password to secure your account."}
                   ($ PasswordInput {:id "password" :full? true :start-icon Lock
                                     :show? show-pass? :toggle! set-show-pass!
                                     :placeholder "At least 8 characters"
                                     :invalid? (and (not (empty? password)) (not pass-valid?))
                                     :value password :on-change #(set-password! (.. % -target -value))}))

                ($ Button {:variant :primary :full? true
                           :disabled (not can-submit?)
                           :on-click #(rf/dispatch [::auth/register {:name name :email email :password password}
                                                    [::nav/home]])
                           :icon UserPlus}
                   "Create account"))

             ($ :div {:class "px-6 pb-6 text-sm text-center text-base-content/70"}
                ($ Shield {:size 14 :class "inline mr-1 opacity-70"})
                ($ :span nil "By creating an account, you agree to our ")
                ($ :a {:href (rfe/href routes/sym-about-route)} "Terms")
                ($ :span nil ". Already have an account? ")
                ($ :a {:href (rfe/href routes/sym-login-route) :class "link link-hover"} "Sign in")))))))
