(ns zi-study.frontend.components.form
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.validation :as validation]))

(defn use-form
  "Create a form state manager with validation
   Options:
   - initial-values: map of initial form values
   - validation-rules: map of field validation rules
   - on-valid-submit: callback when form is valid and submitted
   
   Returns a map with:
   - values: current form values
   - errors: validation errors
   - valid?: whether form is valid
   - set-value: function to set a field value
   - set-values: function to set multiple field values
   - reset-form: function to reset the form
   - handle-submit: function to handle form submission
   - handle-change: function to handle input change events"
  [{:keys [initial-values validation-rules on-valid-submit]}]
  (let [form-state (r/atom {:values (or initial-values {})
                            :errors {}
                            :dirty-fields #{}
                            :touched-fields #{}
                            :submitted? false})
        
        validate (fn [values]
                   (validation/validate-form values validation-rules))
        
        run-validation (fn [field-name]
                         (when (and field-name
                                    (contains? (:touched-fields @form-state) field-name)
                                    (contains? validation-rules field-name))
                           (let [validation-result (validate (:values @form-state))]
                             (swap! form-state assoc :errors (:errors validation-result)))))
        
        set-value (fn [field value]
                    (swap! form-state (fn [state]
                                        (-> state
                                            (assoc-in [:values field] value)
                                            (update :dirty-fields conj field)
                                            (update :touched-fields conj field))))
                    (run-validation field))
        
        set-values (fn [values-map]
                     (swap! form-state (fn [state]
                                         (-> state
                                             (update :values merge values-map)
                                             (update :dirty-fields #(apply conj % (keys values-map)))
                                             (update :touched-fields #(apply conj % (keys values-map))))))
                     (run-validation nil))
        
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
        
        handle-submit (fn [e]
                        (.preventDefault e)
                        (swap! form-state assoc :submitted? true)
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
     :dirty? (not (empty? (:dirty-fields @form-state)))
     :touched? (not (empty? (:touched-fields @form-state)))
     :set-value set-value
     :set-values set-values
     :reset-form reset-form
     :handle-submit handle-submit
     :handle-change handle-change}))

(defn form-field
  "A wrapper component for form fields with error handling"
  [{:keys [field form component component-props]}]
  (let [error (get-in (:errors form) [field 0])]
    [component 
     (merge component-props
            {:value (get-in (:values form) [field])
             :on-change #((:set-value form) field (.. % -target -value))
             :error-message error})]))

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
             :style {:width (str (* (inc strength) 20) "%")}}]]]))