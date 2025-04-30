(ns zi-study.frontend.pages.register
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.components.input :refer [text-input]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.card :refer [card card-content card-footer]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.components.avatar-upload :refer [avatar-upload]]
            ["lucide-react" :as lucide]))

(defn calculate-password-strength [password]
  (if (or (nil? password) (empty? password))
    0
    (let [has-lowercase (re-find #"[a-z]" password)
          has-uppercase (re-find #"[A-Z]" password)
          has-digit (re-find #"[0-9]" password)
          has-special (re-find #"[^a-zA-Z0-9]" password)
          is-long-enough (>= (count password) 8)

          score (+ (if has-lowercase 1 0)
                   (if has-uppercase 1 0)
                   (if has-digit 1 0)
                   (if has-special 1 0)
                   (if is-long-enough 1 0))]

      ;; Return a score from 0-4
      (cond
        (<= score 1) 0  ;; Very weak
        (= score 2) 1   ;; Weak
        (= score 3) 2   ;; Medium
        (= score 4) 3   ;; Strong
        :else 4))))    ;; Very strong

(defn password-strength-indicator [{:keys [strength]}]
  (let [[color text] (case strength
                       0 ["bg-[var(--color-error)]" "Very Weak"]
                       1 ["bg-[var(--color-warning)]" "Weak"]
                       2 ["bg-[var(--color-warning-600)]" "Medium"]
                       3 ["bg-[var(--color-success-600)]" "Strong"]
                       4 ["bg-[var(--color-success)]" "Very Strong"])]

    [:div {:class "mt-1 mb-4"}
     [:div {:class "flex justify-between items-center mb-1"}
      [:span {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
       "Password strength:"]

      [:span {:class "text-xs font-medium"
              :style {:color (case strength
                               0 "var(--color-error)"
                               1 "var(--color-warning)"
                               2 "var(--color-warning-600)"
                               3 "var(--color-success-600)"
                               4 "var(--color-success)")}}
       text]]

     [:div {:class "w-full h-1.5 bg-[var(--color-light-divider)] dark:bg-[var(--color-dark-divider)] rounded-full overflow-hidden"}
      [:div {:class (str "h-full rounded-full transition-all duration-300 " color)
             :style {:width (str (* strength 25) "%")}}]]]))

(defn register-page []
  (let [email (r/atom "")
        password (r/atom "")
        confirm-password (r/atom "")
        first-name (r/atom "")
        last-name (r/atom "")
        profile-image-url (r/atom nil)
        image-preview (r/atom nil)

        loading (r/atom false)
        error (r/atom nil)

        password-strength (r/atom 0)

        update-password-strength (fn [new-password]
                                   (reset! password new-password)
                                   (reset! password-strength (calculate-password-strength new-password)))

        passwords-match? (fn []
                           (and (not (empty? @password))
                                (= @password @confirm-password)))

        handle-image-selected (fn [uploaded-url]
                                (reset! profile-image-url uploaded-url)
                                (reset! image-preview uploaded-url))

        handle-register (fn [e]
                          (.preventDefault e)
                          (reset! loading true)
                          (reset! error nil)

                          (let [registration-data {:email @email
                                                   :password @password
                                                   :first_name @first-name
                                                   :last_name @last-name
                                                   :profile_picture_url @profile-image-url}]

                            (-> (js/fetch "/api/auth/register"
                                          (clj->js {:method "POST"
                                                    :headers {"Content-Type" "application/json"}
                                                    :body (js/JSON.stringify (clj->js registration-data))}))
                                (.then (fn [response]
                                         (.json response)))
                                (.then (fn [data]
                                         (reset! loading false)
                                         (if (.-error data)
                                           (reset! error (.-error data))
                                           (do
                                             (js/alert "Account created successfully! Please sign in.")
                                             (rfe/push-state :zi-study.frontend.core/login)))))
                                (.catch (fn [err]
                                          (reset! loading false)
                                          (reset! error "Network error. Please try again later.")
                                          (js/console.error "Registration error:" err))))))

        valid-form? (fn []
                      (and (not (empty? @email))
                           (not (empty? @password))
                           (not (empty? @confirm-password))
                           (>= @password-strength 2)
                           (passwords-match?)))]

    (fn []
      [:div {:class "flex flex-col items-center justify-center min-h-[80vh] px-4 py-8 animate-fade-in"}

       ;; Title
       [:div {:class "text-center mb-8"}
        [:h1 {:class "text-2xl font-semibold text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
         "Create an Account"]]

       ;; Registration card
       [card {:variant :elevated
              :elevation 2
              :hover-effect true
              :class "w-full max-w-md animate-fade-in-up"}

        [card-content {}

         ;; Error message (if any)
         (when @error
           [alert {:variant :soft
                   :color :error
                   :dismissible true
                   :on-dismiss #(reset! error nil)
                   :class "mb-4"}
            @error])

         ;; Registration form
         [:form {:on-submit handle-register}

          ;; Profile picture upload
          [:div {:class "mb-6"}
           [:div {:class "flex flex-col items-center"}
            [avatar-upload {:src @image-preview
                            :alt "Profile picture"
                            :size :xl
                            :variant :circle
                            :color :primary
                            :disabled @loading
                            :on-image-selected handle-image-selected}]

            [:p {:class "mt-2 text-sm text-center text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
             "Click on the avatar to upload your profile picture"]]]

          ;; Name fields (side by side)
          [:div {:class "grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4"}
           ;; First name
           [text-input {:type :text
                        :variant :outlined
                        :label "First Name"
                        :placeholder "Your first name"
                        :value @first-name
                        :on-change #(reset! first-name (.. % -target -value))
                        :start-icon lucide/User
                        :disabled @loading}]

           ;; Last name
           [text-input {:type :text
                        :variant :outlined
                        :label "Last Name"
                        :placeholder "Your last name"
                        :value @last-name
                        :on-change #(reset! last-name (.. % -target -value))
                        :start-icon lucide/User
                        :disabled @loading}]]

          ;; Email
          [text-input {:type :email
                       :variant :outlined
                       :label "Email"
                       :placeholder "Enter your email"
                       :value @email
                       :on-change #(reset! email (.. % -target -value))
                       :start-icon lucide/Mail
                       :required true
                       :disabled @loading
                       :class "mb-4"}]

          ;; Password
          [text-input {:type :password
                       :variant :outlined
                       :label "Password"
                       :placeholder "Create a password"
                       :value @password
                       :on-change #(update-password-strength (.. % -target -value))
                       :helper-text "Use 8+ characters with a mix of letters, numbers & symbols"
                       :start-icon lucide/Lock
                       :required true
                       :disabled @loading
                       :class "mb-1"}]

          ;; Password strength indicator
          [password-strength-indicator {:strength @password-strength}]

          ;; Confirm password
          [text-input {:type :password
                       :variant :outlined
                       :label "Confirm Password"
                       :placeholder "Confirm your password"
                       :value @confirm-password
                       :on-change #(reset! confirm-password (.. % -target -value))
                       :error-text (when (and (not (empty? @confirm-password))
                                              (not (= @password @confirm-password)))
                                     "Passwords do not match")
                       :start-icon lucide/Lock
                       :required true
                       :disabled @loading
                       :class "mb-6"}]

          ;; Register button
          [button {:variant :primary
                   :size :lg
                   :full-width true
                   :start-icon lucide/UserPlus
                   :disabled (or (not (valid-form?)) @loading)
                   :class "mb-4"}

           (if @loading
             [:span
              [:span {:class "inline-block animate-spin mr-2"}
               [:> lucide/Loader {:size 16}]]
              "Creating account..."]
             "Create account")]]]

        ;; Card footer with login link
        [card-footer {:align :center}
         [:div {:class "w-full"}
          [:div {:class "text-center mb-4 text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
           "Already have an account?"]

          ;; Login button
          [button {:variant :outlined
                   :size :lg
                   :full-width true
                   :start-icon lucide/LogIn
                   :disabled @loading
                   :on-click #(rfe/push-state :zi-study.frontend.core/login)}
           "Sign in"]]]]])))