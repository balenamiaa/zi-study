(ns frontend.pages.login
  (:require ["lucide-react" :refer [LogIn Mail Lock KeyRound ShieldCheck]]
            [frontend.routes :as routes]
            [frontend.state.auth :as auth]
            [frontend.state.nav :as nav]
            [frontend.ui.button :refer [Button]]
            [frontend.ui.card :refer [Card]]
            [frontend.ui.field :refer [FieldWrapper TextInput PasswordInput Checkbox]]
            [frontend.ui.feedback :as feedback]
            [frontend.uix.hooks :refer [use-subscribe]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(defui login-page [{:keys [query-params]}]
  (let [[email set-email!] (uix/use-state "")
        [password set-password!] (uix/use-state "")
        [show-pass? set-show-pass!] (uix/use-state false)
        [remember? set-remember!] (uix/use-state true)
        redirect-to (get query-params :redirect)
        on-success (if redirect-to [::nav/redirect redirect-to] [::nav/home])
        auth (use-subscribe [::auth/auth])
        error (:error auth)
        email-valid? (boolean (re-find #".+@.+" email))
        pass-valid? (pos? (count password))
        can-submit? (and email-valid? pass-valid?)]
    (uix/use-effect
     (fn []
       (when (= error :invalid-credentials)
         (feedback/toast! {:text "Invalid email or password" :variant :error}))) [error])
    ($ :div {:class "relative min-h-[80vh] grid place-items-center px-4"}
       ($ :div {:class "absolute inset-0 -z-10 pointer-events-none"}
          ($ :div {:class "absolute -top-24 -left-24 w-72 h-72 bg-primary/20 rounded-full blur-3xl"})
          ($ :div {:class "absolute -bottom-24 -right-24 w-72 h-72 bg-secondary/20 rounded-full blur-3xl"}))

       ($ :div {:class "w-full max-w-md"}
          ($ :div {:class "text-center mb-6 space-y-2"}
             ($ :div {:class "inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-primary/10 text-primary shadow-md"}
                ($ ShieldCheck {:size 24}))
             ($ :h1 {:class "text-heading-2"} "Welcome back")
             ($ :p {:class "text-body text-base-content/70"} "Sign in to continue your journey."))

          ($ Card {:glass? true}
             ($ :div {:class "space-y-5"}
                ($ FieldWrapper {:id "email" :label "Email"
                                 :hint (when (and (not (empty? email)) (not email-valid?))
                                         "Enter a valid email")}
                   ($ TextInput {:id "email" :full? true :start-icon Mail
                                 :type "email"
                                 :placeholder "you@domain.com"
                                 :invalid? (and (not (empty? email)) (not email-valid?))
                                 :value email :on-change #(set-email! (.. % -target -value))}))

                ($ FieldWrapper {:id "password" :label "Password"
                                 :hint (when (and (not (empty? password)) (not pass-valid?))
                                         "Password can't be empty.")}
                   ($ PasswordInput {:id "password" :full? true
                                     :show? show-pass? :toggle! set-show-pass!
                                     :start-icon Lock
                                     :placeholder "••••••••"
                                     :invalid? (and (not (empty? password)) (not pass-valid?))
                                     :value password :on-change #(set-password! (.. % -target -value))}))

                ($ Checkbox {:label "Remember me for 30 days"
                             :checked remember?
                             :on-change #(set-remember! (.. % -target -checked))})

                ($ Button {:variant :primary :full? true
                           :disabled (not can-submit?)
                           :on-click #(rf/dispatch [::auth/login {:email email :password password :remember? remember?} on-success])
                           :icon LogIn}
                   "Sign in")

                ($ :div {:class "divider"} "or")
                ($ :a {:href "/api/auth/google/start" :class "btn btn-outline w-full"}
                   ($ KeyRound {:size 18})
                   ($ :span {:class "ml-2"} "Continue with Google")))

             ($ :div {:class "px-6 pb-6 text-sm text-center text-base-content/70"}
                ($ :a {:href "#" :on-click #(.preventDefault %)} "Forgot password?")
                ($ :span {:class "mx-1"} "·")
                ($ :a {:href (rfe/href routes/sym-register-route)} "Create an account")))))))
