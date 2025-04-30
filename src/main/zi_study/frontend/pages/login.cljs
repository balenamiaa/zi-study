(ns zi-study.frontend.pages.login
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.components.input :refer [text-input]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.card :refer [card card-header card-content card-footer]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.auth :as auth]
            ["lucide-react" :as lucide-icons]))

(defn email-valid? [email]
  (re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$" email))

(defn login-page []
  (let [email (r/atom "")
        password (r/atom "")
        show-password (r/atom false)
        loading (r/atom false)
        error (r/atom nil)

        toggle-password-visibility (fn [e]
                                     (.preventDefault e)
                                     (.stopPropagation e)
                                     (swap! show-password not))

        handle-login (fn [e]
                       (.preventDefault e)
                       (reset! error nil)
                       (reset! loading true)

                       (auth/login @email @password
                                   (fn [result]
                                     (reset! loading false)
                                     (if (:success result)
                                       (do
                                         (state/set-authenticated true (:token result) (:user result))
                                         (rfe/push-state :zi-study.frontend.core/home))
                                       (reset! error (:error result))))))

        valid-form? #(and (not (empty? @email))
                          (not (empty? @password))
                          (email-valid? @email))]

    (fn []
      [:div {:class "flex flex-col items-center justify-center min-h-[80vh] px-4 py-8 animate-fade-in"}

       ;; Logo and title
       [:div {:class "text-center mb-8"}
        [:div {:class "flex items-center justify-center mb-4"}
         [:div {:class "text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
          "ZiStudy"]
         [:> lucide-icons/Sparkles {:size 24 :className "ml-2 text-[var(--color-secondary)]"}]]]

       ;; Login card
       [card {:variant :elevated
              :elevation 2
              :hover-effect true
              :class "w-full max-w-md animate-fade-in-up"}

        [card-header {:title "Login" :icon lucide-icons/LogIn}]

        [card-content {}

         ;; Error message (if any)
         (when @error
           [alert {:variant :soft
                   :color :error
                   :dismissible true
                   :on-dismiss #(reset! error nil)
                   :class "mb-4"}
            @error])

         ;; Login form
         [:form {:on-submit handle-login}

          ;; Email input
          [text-input {:type :email
                       :variant :outlined
                       :label "Email"
                       :placeholder "Enter your email"
                       :value @email
                       :on-change #(reset! email (.. % -target -value))
                       :start-icon lucide-icons/Mail
                       :required true
                       :disabled @loading
                       :class "mb-4"}]

          ;; Password input with visibility toggle
          [text-input {:type (if @show-password :text :password)
                       :variant :outlined
                       :label "Password"
                       :placeholder "Enter your password"
                       :value @password
                       :on-change #(reset! password (.. % -target -value))
                       :start-icon lucide-icons/Lock
                       :end-icon (if @show-password lucide-icons/EyeOff lucide-icons/Eye)
                       :on-end-icon-click toggle-password-visibility
                       :required true
                       :disabled @loading
                       :class "mb-6"}]

          ;; Login button
          [button {:variant :primary
                   :size :lg
                   :full-width true
                   :start-icon lucide-icons/LogIn
                   :disabled (or (not (valid-form?)) @loading)
                   :class "mb-4"}

           (if @loading
             [:span
              [:span {:class "inline-block animate-spin mr-2"}
               [:> lucide-icons/Loader {:size 16}]]
              "Signing in..."]
             "Sign in")]]]

        ;; Card footer with register link
        [card-footer {:align :center}
         [:div {:class "w-full"}
          [:div {:class "text-center mb-4 text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
           "Don't have an account?"]

          ;; Register button
          [button {:variant :outlined
                   :size :lg
                   :full-width true
                   :start-icon lucide-icons/UserPlus
                   :disabled @loading
                   :on-click #(rfe/push-state :zi-study.frontend.core/register)}
           "Create account"]]]]

       ;; Optional: Additional help links
       [:div {:class "mt-6 text-center text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
        [:a {:class "hover:underline" :href "#"} "Forgot password?"]
        [:span {:class "mx-2"} "•"]
        [:a {:class "hover:underline" :href "#"} "Need help?"]]])))