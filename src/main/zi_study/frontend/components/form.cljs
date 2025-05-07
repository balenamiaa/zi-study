(ns zi-study.frontend.components.form
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.validation :as validation]
            [zi-study.frontend.utilities :refer [cx]]
            ["lucide-react" :as lucide-icons]))

(defn use-form
  "Create a form state manager with validation
   Options:
   - initial-values: map of initial form values
   - validation-rules: map of field validation rules
   - on-valid-submit: callback when form is valid and submitted
   - validate-on-change: boolean (default true) - run validation on each change
   - validate-on-blur: boolean (default true) - run validation on blur
   
   Returns a map with:
   - values: current form values
   - errors: validation errors
   - valid?: whether form is valid
   - set-value: function to set a field value
   - set-values: function to set multiple field values
   - reset-form: function to reset the form
   - handle-submit: function to handle form submission
   - handle-change: function to handle input change events
   - handle-blur: function to handle input blur events
   - mark-field-touched: function to mark a field as touched"
  [{:keys [initial-values validation-rules on-valid-submit validate-on-change validate-on-blur]
    :or {validate-on-change true
         validate-on-blur true}}]
  (let [form-state (r/atom {:values (or initial-values {})
                            :errors {}
                            :dirty-fields #{}
                            :touched-fields #{}
                            :submitted? false})

        validate (fn [values]
                   (validation/validate-form values validation-rules))

        run-validation (fn [field-name]
                         (let [validation-result (validate (:values @form-state))]
                           (if field-name
                             ;; Validate only specified field if it's touched or form is submitted
                             (when (or (:submitted? @form-state)
                                       (contains? (:touched-fields @form-state) field-name))
                               (swap! form-state update :errors
                                      #(assoc % field-name (get-in validation-result [:errors field-name]))))
                             ;; Validate all fields
                             (swap! form-state assoc :errors (:errors validation-result)))))

        set-value (fn [field value]
                    (swap! form-state (fn [state]
                                        (-> state
                                            (assoc-in [:values field] value)
                                            (update :dirty-fields conj field))))
                    (when validate-on-change
                      (run-validation field)))

        set-values (fn [values-map]
                     (swap! form-state (fn [state]
                                         (-> state
                                             (update :values merge values-map)
                                             (update :dirty-fields #(apply conj % (keys values-map))))))
                     (when validate-on-change
                       (run-validation nil)))

        mark-field-touched (fn [field]
                             (swap! form-state update :touched-fields conj field)
                             (when validate-on-blur
                               (run-validation field)))

        reset-form (fn []
                     (reset! form-state {:values (or initial-values {})
                                         :errors {}
                                         :dirty-fields #{}
                                         :touched-fields #{}
                                         :submitted? false}))

        handle-change (fn [field]
                        (fn [e]
                          (let [target-value (.. e -target -value)]
                            (set-value field target-value))))

        handle-blur (fn [field]
                      (fn [_]
                        (mark-field-touched field)))

        handle-submit (fn [e]
                        (.preventDefault e)
                        (swap! form-state (fn [state]
                                            (-> state
                                                (assoc :submitted? true)
                                                (update :touched-fields #(apply conj % (keys validation-rules))))))
                        (let [values (:values @form-state)
                              validation-result (validate values)]
                          (swap! form-state assoc :errors (:errors validation-result))
                          (when (:valid? validation-result)
                            (when on-valid-submit
                              (on-valid-submit values)))))]

    {:values (:values @form-state)
     :errors (:errors @form-state)
     :valid? (empty? (:errors @form-state))
     :submitted? (:submitted? @form-state)
     :dirty? (seq (:dirty-fields @form-state))
     :touched? (seq (:touched-fields @form-state))
     :set-value set-value
     :set-values set-values
     :reset-form reset-form
     :handle-submit handle-submit
     :handle-change handle-change
     :handle-blur handle-blur
     :mark-field-touched mark-field-touched}))

(defn form-field
  "A wrapper component for form fields with error handling
   
   Options:
   - field: field name in the form state
   - form: form state from use-form
   - component: the input component to render
   - component-props: props to pass to the component
   - label: field label text
   - required: true/false if field is required
   - helper-text: helper text to display
   - class: additional CSS classes for wrapper"
  [{:keys [field form component component-props label required helper-text class]}]
  (let [error (get-in (:errors form) [field])
        error-message (when (seq error) (first error))
        is-touched (contains? (get-in form [:touched-fields]) field)
        is-submitted (:submitted? form)
        show-error (and error-message (or is-touched is-submitted))

        field-id (or (:id component-props) (str "field-" (name field)))]

    [:div {:class (cx "mb-4" class)}
     ;; Label
     (when label
       [:label {:for field-id
                :class (cx "block mb-1 text-sm font-medium"
                           (if show-error
                             "text-[var(--color-error)] dark:text-[var(--color-error-300)]"
                             "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"))}
        label
        (when required
          [:span {:class "ml-1 text-[var(--color-error)]"} "*"])])

     ;; Input component
     [component
      (merge {:id field-id
              :name (name field)
              :value (get-in (:values form) [field])
              :on-change #((:set-value form) field (.. % -target -value))
              :on-blur #((:mark-field-touched form) field)
              :aria-invalid (boolean show-error)
              :aria-describedby (when (or show-error helper-text)
                                  (str field-id "-description"))}
             component-props)]

     ;; Helper text or error message
     (cond
       show-error
       [:div {:id (str field-id "-description")
              :class (cx "mt-1.5 text-xs flex items-center gap-1"
                         "text-[var(--color-error)] dark:text-[var(--color-error-300)]")}
        [:> lucide-icons/AlertCircle {:size 12 :className "flex-shrink-0"}]
        error-message]

       helper-text
       [:div {:id (str field-id "-description")
              :class "mt-1.5 text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
        helper-text])]))

(defn password-strength-indicator
  "A visual indicator for password strength
   
   Options:
   - strength: number from 0-4 indicating strength
   - show-text: true/false whether to show text label (default true)
   - class: additional CSS classes"
  [{:keys [strength show-text class] :or {show-text true}}]
  (let [[color text] (case strength
                       0 ["bg-[var(--color-error)]" "Very Weak"]
                       1 ["bg-[var(--color-warning)]" "Weak"]
                       2 ["bg-[var(--color-warning-600)]" "Medium"]
                       3 ["bg-[var(--color-success-600)]" "Strong"]
                       4 ["bg-[var(--color-success)]" "Very Strong"]
                       ["bg-[var(--color-light-divider)]" ""])]

    [:div {:class (cx "mt-1.5 mb-4" class)}
     (when show-text
       [:div {:class "flex justify-between items-center mb-1.5"}
        [:span {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
         "Password strength:"]

        [:span {:class (cx "text-xs font-medium transition-colors"
                           (case strength
                             0 "text-[var(--color-error)] dark:text-[var(--color-error-300)]"
                             1 "text-[var(--color-warning)] dark:text-[var(--color-warning-300)]"
                             2 "text-[var(--color-warning-600)] dark:text-[var(--color-warning-400)]"
                             3 "text-[var(--color-success-600)] dark:text-[var(--color-success-400)]"
                             4 "text-[var(--color-success)] dark:text-[var(--color-success-300)]"
                             "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"))}
         text]])

     [:div {:class "w-full h-1.5 bg-[var(--color-light-divider)] dark:bg-[var(--color-dark-divider)] rounded-full overflow-hidden grid grid-cols-5 gap-0.5"
            :role "progressbar"
            :aria-valuenow (inc strength)
            :aria-valuemin "0"
            :aria-valuemax "5"
            :aria-valuetext text}
      (for [i (range 5)]
        ^{:key (str "strength-segment-" i)}
        [:div {:class (cx "h-full rounded-full transition-all duration-300"
                          (if (<= i strength) color "bg-transparent"))}])]]))

(defn form-section
  "A section within a form with optional title and description
   
   Options:
   - title: section title
   - description: section description text
   - class: additional CSS classes"
  [{:keys [title description class]} & children]
  [:fieldset {:class (cx "mb-6 pb-6 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]" class)}
   (when (or title description)
     [:div {:class "mb-4"}
      (when title
        [:h3 {:class "text-lg font-medium text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
         title])
      (when description
        [:p {:class "mt-1 text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
         description])])

   (into [:div] children)])