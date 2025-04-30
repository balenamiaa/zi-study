(ns zi-study.frontend.pages.login
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.components.input :refer [text-input]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.card :refer [card card-header card-content card-footer]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.state :refer [app-state]]
            ["lucide-react" :as lucide-icons]
            [goog.object :as gobj]))

(defn login-page []
  (let [email (r/atom "")
        password (r/atom "")
        show-password (r/atom false)
        loading (r/atom false)
        error (r/atom nil)

        ;; Basic email validation regex
        email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
        is-valid-email? (fn [email-str]
                          (re-matches email-regex email-str))

        toggle-password-visibility (fn [e]
                                     (.preventDefault e)
                                     (.stopPropagation e)
                                     (swap! show-password not))

        handle-login (fn [e]
                       (.preventDefault e)
                       (reset! error nil)
                       (reset! loading true)

                       (-> (js/fetch "/api/auth/login"
                                     (clj->js {:method "POST"
                                               :headers {"Content-Type" "application/json"}
                                               :body (js/JSON.stringify #js {:email @email :password @password})}))
                           (.then (fn [response]
                                    (if (.-ok response)
                                      (.json response)
                                      (js/Promise.reject "Invalid email or password"))))
                           (.then (fn [data]
                                    (reset! loading false)
                                    (if-let [token (gobj/get data "token")]
                                      (let [user (gobj/get data "user")]
                                        (.setItem js/localStorage "auth-token" token)

                                        ;; Update central app state
                                        (swap! app-state assoc
                                               :auth/authenticated? true
                                               :auth/token token
                                               :auth/current-user (js->clj user :keywordize-keys true)
                                               :auth/loading? false)

                                        ;; Redirect to home page
                                        (rfe/push-state :zi-study.frontend.core/home))
                                      ;; Login failed (no token/user in response)
                                      (reset! error (or (gobj/get data "error") "Login failed. Please check your credentials.")))))
                           (.catch (fn [err]
                                     (reset! loading false)
                                     (reset! error (if (string? err)
                                                     err
                                                     (or (gobj/get err "message") "An unexpected error occurred.")))))))

        valid-form? (fn []
                      (and (not (empty? @email))
                           (not (empty? @password))
                           (is-valid-email? @email)))]

    (fn []
      [:div {:class "flex flex-col items-center justify-center min-h-[80vh] px-4 py-8 animate-fade-in"}

       ;; Logo and title
       [:div {:class "text-center mb-8"}
        [:div {:class "flex items-center justify-center mb-4"}
         [:div {:class "text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
          "ZiStudy"]
         [:> lucide-icons/Sparkles {:size 24 :className "ml-2 text-[var(--color-secondary)]"}]]

        [:h1 {:class "text-2xl font-semibold text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
         "Welcome Back"]

        [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mt-2"}
         "Sign in to continue to your account"]]

       ;; Login card
       [card {:variant :elevated
              :elevation 2
              :hover-effect true
              :class "w-full max-w-md animate-fade-in-up"}

        [card-header
         [:h2 {:class "text-xl font-semibold"} "Login"]]

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