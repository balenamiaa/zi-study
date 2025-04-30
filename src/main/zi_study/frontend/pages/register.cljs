(ns zi-study.frontend.pages.register
  (:require
   ["lucide-react" :as lucide-icons]
   [clojure.string :as str]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [zi-study.frontend.components.alert :refer [alert]]
   [zi-study.frontend.components.avatar-upload :refer [avatar-upload]]
   [zi-study.frontend.components.button :refer [button]]
   [zi-study.frontend.components.card :refer [card card-content card-footer
                                              card-header]]
   [zi-study.frontend.components.input :refer [text-input]]
   [zi-study.frontend.state :as state]
   [zi-study.frontend.utilities.auth :as auth]
   [zi-study.frontend.utilities.validation :as validation]))

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
  (let [form-data (r/atom {:first-name ""
                           :last-name ""
                           :email ""
                           :password ""
                           :confirm-password ""
                           :profile_picture_url nil})
        image-preview (r/atom nil)
        loading (r/atom false)
        error (r/atom nil)
        success (r/atom nil)
        show-password (r/atom false)
        show-confirm-password (r/atom false)

        handle-image-selected (fn [uploaded-url]
                                (swap! form-data assoc :profile_picture_url uploaded-url)
                                (reset! image-preview uploaded-url))

        validation-rules {:first-name [[validation/required? [] "First name is required"]
                                       [validation/max-length? [50] "First name cannot exceed 50 characters"]]
                          :last-name [[validation/required? [] "Last name is required"]
                                      [validation/max-length? [50] "Last name cannot exceed 50 characters"]]
                          :email [[validation/required? [] "Email is required"]
                                  [validation/email-valid? [] "Please enter a valid email"]]
                          :password [[validation/required? [] "Password is required"]
                                     [validation/min-length? [8] "Password must be at least 8 characters"]
                                     [validation/password-valid? [] "Password must contain at least one letter and one number"]]
                          :confirm-password [[validation/required? [] "Please confirm your password"]
                                             [(fn [value] (= value (:password @form-data))) [] "Passwords don't match"]]}

        toggle-password-visibility (fn [field e]
                                     (.preventDefault e)
                                     (.stopPropagation e)
                                     (case field
                                       :password (swap! show-password not)
                                       :confirm-password (swap! show-confirm-password not)))

        update-field (fn [field e]
                       (let [value (.. e -target -value)]
                         (swap! form-data assoc field value)
                         ;; If password field is updated, recalculate strength
                         (when (= field :password)
                           (calculate-password-strength value))))

        validate-form (fn []
                        (validation/validate-form @form-data validation-rules))

        handle-register (fn [e]
                          (.preventDefault e)
                          (reset! error nil)
                          (reset! success nil)

                          (let [validation-result (validate-form)]
                            (if (:valid? validation-result)
                              (do
                                (reset! loading true)
                                (auth/register (dissoc @form-data :confirm-password)
                                               (fn [result]
                                                 (reset! loading false)
                                                 (if (:success result)
                                                   (reset! success "Registration successful! You can now login.")
                                                   (reset! error (:error result))))))
                              ;; Combine error messages for clarity
                              (reset! error (str "Please fix the form errors before submitting: "
                                                 (str/join ", " (mapcat second (:errors validation-result))))))))]

    (fn []
      (let [validation-result (validate-form)
            field-errors (:errors validation-result)]

        [:div {:class "flex flex-col items-center justify-center min-h-[80vh] px-4 py-8 animate-fade-in"}

         [:div {:class "text-center mb-8"}
          [:div {:class "flex items-center justify-center mb-4"}
           [:div {:class "text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
            "ZiStudy"]
           [:> lucide-icons/Sparkles {:size 24 :className "ml-2 text-[var(--color-secondary)]"}]]

          [:h1 {:class "text-2xl font-semibold text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
           "Create Account"]]

         [card {:variant :elevated
                :elevation 2
                :hover-effect true
                :class "w-full max-w-md animate-fade-in-up"}

          [card-header {:title "Register" :icon lucide-icons/UserPlus}]

          [card-content {}

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

           (when @error
             [alert {:variant :soft
                     :color :error
                     :dismissible true
                     :on-dismiss #(reset! error nil)
                     :class "mb-4"}
              @error])

           (when @success
             [alert {:variant :soft
                     :color :success
                     :dismissible true
                     :on-dismiss #(reset! success nil)
                     :class "mb-4"}
              @success])

           [:form {:on-submit handle-register}

            [:div {:class "flex flex-col md:flex-row gap-4 mb-4"}
             [:div {:class "flex-1"}
              [text-input {:type :text
                           :variant :outlined
                           :label "First Name"
                           :placeholder "Enter your first name"
                           :value (:first-name @form-data)
                           :on-change #(update-field :first-name %)
                           :start-icon lucide-icons/User
                           :required true
                           :error-text (first (get field-errors :first-name))
                           :disabled @loading}]]

             [:div {:class "flex-1"}
              [text-input {:type :text
                           :variant :outlined
                           :label "Last Name"
                           :placeholder "Enter your last name"
                           :value (:last-name @form-data)
                           :on-change #(update-field :last-name %)
                           :start-icon lucide-icons/User
                           :required true
                           :error-text (first (get field-errors :last-name))
                           :disabled @loading}]]]

            [text-input {:type :email
                         :variant :outlined
                         :label "Email"
                         :placeholder "Enter your email"
                         :value (:email @form-data)
                         :on-change #(update-field :email %)
                         :start-icon lucide-icons/Mail
                         :required true
                         :error-text (first (get field-errors :email))
                         :disabled @loading
                         :class "mb-4"}]

            [text-input {:type (if @show-password :text :password)
                         :variant :outlined
                         :label "Password"
                         :placeholder "Create a strong password"
                         :value (:password @form-data)
                         :on-change #(update-field :password %)
                         :start-icon lucide-icons/Lock
                         :end-icon (if @show-password lucide-icons/EyeOff lucide-icons/Eye)
                         :on-end-icon-click #(toggle-password-visibility :password %)
                         :required true
                         :error-text (first (get field-errors :password))
                         :disabled @loading
                         :class "mb-4"}]

            [password-strength-indicator {:strength (calculate-password-strength (:password @form-data))}]

            [text-input {:type (if @show-confirm-password :text :password)
                         :variant :outlined
                         :label "Confirm Password"
                         :placeholder "Confirm your password"
                         :value (:confirm-password @form-data)
                         :on-change #(update-field :confirm-password %)
                         :start-icon lucide-icons/Shield
                         :end-icon (if @show-confirm-password lucide-icons/EyeOff lucide-icons/Eye)
                         :on-end-icon-click #(toggle-password-visibility :confirm-password %)
                         :required true
                         :error-text (first (get field-errors :confirm-password))
                         :disabled @loading
                         :class "mb-6"}]

            [button {:variant :primary
                     :size :lg
                     :full-width true
                     :start-icon lucide-icons/UserPlus
                     :disabled (or (not (:valid? validation-result)) @loading)
                     :class "mb-4"}

             (if @loading
               [:span
                [:span {:class "inline-block animate-spin mr-2"}
                 [:> lucide-icons/Loader {:size 16}]]
                "Creating account..."]
               "Create account")]]]

          [card-footer {:align :center}
           [:div {:class "w-full"}
            [:div {:class "text-center mb-4 text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
             "Already have an account?"]

            [button {:variant :outlined
                     :size :lg
                     :full-width true
                     :start-icon lucide-icons/LogIn
                     :disabled @loading
                     :on-click #(rfe/push-state :zi-study.frontend.core/login)}
             "Sign in"]]]]

         [:div {:class "mt-6 text-center text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
          [:span "By registering, you agree to our "]
          [:a {:class "hover:underline text-[var(--color-primary)]" :href "#"} "Terms of Service"]
          [:span " and "]
          [:a {:class "hover:underline text-[var(--color-primary)]" :href "#"} "Privacy Policy"]]]))))