(ns zi-study.frontend.components.toggle
  (:require
   [reagent.core :as r]))

(defn toggle
  "A toggle switch component for boolean selection.

   Options:
   - checked: true/false - current state of the toggle (required)
   - on-change: function to call when toggle is clicked (receives event)
   - size: :sm, :md, :lg (default :md) - size of the toggle
   - color: :primary, :secondary, :success (default :primary) - color theme
   - label: text label for the toggle
   - label-position: :left, :right (default :right) - label position
   - disabled: true/false (default false) - disables the toggle
   - class: additional CSS classes for the container
   - input-class: additional CSS classes for the input element"
  [{:keys [checked on-change size color label label-position disabled class input-class]
    :or {size :md
         color :primary
         label-position :right
         disabled false}}]

  (let [base-classes "relative inline-flex items-center"

        toggle-base (str "peer relative appearance-none rounded-full transition-colors duration-300 "
                         "ease-in-out focus:outline-none focus:ring-2 focus:ring-offset-2 "
                         "disabled:opacity-50 disabled:cursor-not-allowed")

        size-classes
        (case size
          :sm {:toggle "w-8 h-4"
               :dot "w-3 h-3 peer-checked:translate-x-5"
               :container "text-sm"}
          :md {:toggle "w-10 h-5"
               :dot "w-4 h-4 peer-checked:translate-x-6"
               :container "text-base"}
          :lg {:toggle "w-12 h-6"
               :dot "w-5 h-5 peer-checked:translate-x-7"
               :container "text-lg"}
          {:toggle "w-10 h-5"
           :dot "w-4 h-4 peer-checked:translate-x-6"
           :container "text-base"})

        color-classes
        (case color
          :primary {:bg "peer-checked:bg-[var(--color-primary)] dark:peer-checked:bg-[var(--color-primary)]"
                    :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-400)]"}
          :secondary {:bg "peer-checked:bg-[var(--color-secondary)] dark:peer-checked:bg-[var(--color-secondary)]"
                      :ring "focus:ring-[var(--color-secondary-light)]"}
          :success {:bg "peer-checked:bg-[var(--color-success)] dark:peer-checked:bg-[var(--color-success)]"
                    :ring "focus:ring-green-300"}
          {:bg "peer-checked:bg-[var(--color-primary)] dark:peer-checked:bg-[var(--color-primary)]"
           :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-400)]"})

        toggle-classes (str toggle-base " "
                            (:toggle size-classes) " "
                            (:ring color-classes) " "
                            "bg-[var(--color-light-text-secondary)] dark:bg-[var(--color-dark-text-secondary)] "
                            (:bg color-classes) " "
                            input-class)

        container-classes (str base-classes " " (:container size-classes) " " class)

        label-classes "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] select-none"

        trigger-change (fn [_]
                         (when on-change
                           (let [fake-event (js-obj)]
                             (on-change fake-event))))]

    [:div {:class container-classes}

     ;; Label before toggle if position is :left
     (when (and label (= label-position :left))
       [:label {:class (str "mr-3 " label-classes " cursor-pointer")
                :on-click trigger-change}
        label])

     ;; Toggle switch
     [:div.relative.flex.items-center.h-full
      [:input {:type "checkbox"
               :role "switch"
               :class toggle-classes
               :checked checked
               :disabled disabled
               :on-change on-change}]

      [:div {:class (str "absolute left-0.5 top-1/2 -translate-y-1/2 rounded-full bg-white "
                         "transition-transform duration-300 ease-in-out "
                         (:dot size-classes) " cursor-pointer")
             :aria-hidden true
             :on-click trigger-change}]]

     ;; Label after toggle if position is :right
     (when (and label (= label-position :right))
       [:label {:class (str "ml-3 " label-classes " cursor-pointer")
                :on-click trigger-change}
        label])]))

(defn checkbox
  "A checkbox component for boolean/indeterminate selection.

   Options:
   - checked: true/false - current state of the checkbox (required)
   - indeterminate: true/false (default false) - indeterminate state
   - on-change: function to call when checkbox is clicked (receives event)
   - size: :sm, :md, :lg (default :md) - size of the checkbox
   - color: :primary, :secondary, :success (default :primary) - color theme
   - label: text label for the checkbox
   - disabled: true/false (default false) - disables the checkbox
   - class: additional CSS classes for the container
   - input-class: additional CSS classes for the input element"
  [{:keys [checked indeterminate on-change size color label disabled class input-class]
    :or {indeterminate false
         size :md
         color :primary
         disabled false}}]

  (let [input-ref (r/atom nil)

        base-classes "relative inline-flex items-center"

        checkbox-base (str "appearance-none transition-colors duration-200 ease-in-out focus:outline-none "
                           "focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed border-2")

        size-classes
        (case size
          :sm {:checkbox "w-4 h-4"
               :container "text-sm"}
          :md {:checkbox "w-5 h-5"
               :container "text-base"}
          :lg {:checkbox "w-6 h-6"
               :container "text-lg"}
          {:checkbox "w-5 h-5"
           :container "text-base"})

        color-classes
        (case color
          :primary {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                    :checked (str "checked:bg-[var(--color-primary)] dark:checked:bg-[var(--color-primary)] "
                                  "checked:border-[var(--color-primary)] dark:checked:border-[var(--color-primary)]")
                    :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-400)]"}
          :secondary {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                      :checked (str "checked:bg-[var(--color-secondary)] dark:checked:bg-[var(--color-secondary)] "
                                    "checked:border-[var(--color-secondary)] dark:checked:border-[var(--color-secondary)]")
                      :ring "focus:ring-[var(--color-secondary-light)]"}
          :success {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                    :checked (str "checked:bg-[var(--color-success)] dark:checked:bg-[var(--color-success)] "
                                  "checked:border-[var(--color-success)] dark:checked:border-[var(--color-success)]")
                    :ring "focus:ring-green-300"}
          {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
           :checked (str "checked:bg-[var(--color-primary)] dark:checked:bg-[var(--color-primary)] "
                         "checked:border-[var(--color-primary)] dark:checked:border-[var(--color-primary)]")
           :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-400)]"})

        checkbox-classes (str checkbox-base " "
                              (:checkbox size-classes) " "
                              (:border color-classes) " "
                              (:checked color-classes) " "
                              (:ring color-classes) " "
                              "rounded bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] "
                              "checked:bg-no-repeat checked:bg-center "
                              input-class)

        container-classes (str base-classes " " (:container size-classes) " " class)

        label-classes "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] select-none"

        ;; Effect to set indeterminate state
        _ (r/create-class
           {:component-did-mount
            (fn []
              (when (and @input-ref indeterminate)
                (set! (.-indeterminate @input-ref) indeterminate)))
            :component-did-update
            (fn [this [_ old-props]]
              (let [[_ new-props] (r/argv this)]
                (when (and @input-ref
                           (or (not= (:indeterminate old-props false)
                                     (:indeterminate new-props false))
                               (some? (:indeterminate new-props))))
                  (set! (.-indeterminate @input-ref) (:indeterminate new-props false)))))
            :reagent-render
            (fn [_] nil)})

        trigger-change (fn [_]
                         (when on-change
                           (let [fake-event (js-obj)]
                             (on-change fake-event))))]

    [:div {:class container-classes}
     [:input {:type "checkbox"
              :class checkbox-classes
              :checked checked
              :disabled disabled
              :on-change on-change
              :ref #(when % (reset! input-ref %))}]

     (when label
       [:label {:class (str "ml-2 " label-classes " cursor-pointer")
                :on-click trigger-change}
        label])]))

(defn radio
  "A radio button component for single selection within a group.

   Options:
   - checked: true/false - current state of the radio button (required)
   - on-change: function to call when radio is clicked (receives event)
   - name: group name for the radio buttons (required)
   - value: value of this radio button (required)
   - size: :sm, :md, :lg (default :md) - size of the radio
   - color: :primary, :secondary, :success (default :primary) - color theme
   - label: text label for the radio button
   - disabled: true/false (default false) - disables the radio button
   - class: additional CSS classes for the container
   - input-class: additional CSS classes for the input element"
  [{:keys [checked on-change name value size color label disabled class input-class]
    :or {size :md
         color :primary
         disabled false}}]

  (let [base-classes "relative inline-flex items-center"

        radio-base (str "appearance-none rounded-full transition-colors duration-200 ease-in-out "
                        "focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 "
                        "disabled:cursor-not-allowed border-2")

        size-classes
        (case size
          :sm {:radio "w-4 h-4"
               :dot "w-2.5 h-2.5"
               :container "text-sm"}
          :md {:radio "w-5 h-5"
               :dot "w-3 h-3"
               :container "text-base"}
          :lg {:radio "w-6 h-6"
               :dot "w-3.5 h-3.5"
               :container "text-lg"}
          {:radio "w-5 h-5"
           :dot "w-3 h-3"
           :container "text-base"})

        color-classes
        (case color
          :primary {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                    :checked "checked:border-[var(--color-primary)] dark:checked:border-[var(--color-primary)]"
                    :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-400)]"
                    :dot "bg-[var(--color-primary)] dark:bg-[var(--color-primary)]"}
          :secondary {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                      :checked "checked:border-[var(--color-secondary)] dark:checked:border-[var(--color-secondary)]"
                      :ring "focus:ring-[var(--color-secondary-light)]"
                      :dot "bg-[var(--color-secondary)] dark:bg-[var(--color-secondary)]"}
          :success {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                    :checked "checked:border-[var(--color-success)] dark:checked:border-[var(--color-success)]"
                    :ring "focus:ring-green-300"
                    :dot "bg-[var(--color-success)] dark:bg-[var(--color-success)]"}
          {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
           :checked "checked:border-[var(--color-primary)] dark:checked:border-[var(--color-primary)]"
           :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-400)]"
           :dot "bg-[var(--color-primary)] dark:bg-[var(--color-primary)]"})

        radio-classes (str radio-base " "
                           (:radio size-classes) " "
                           (:border color-classes) " "
                           (:checked color-classes) " "
                           (:ring color-classes) " "
                           "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] "
                           input-class)

        container-classes (str base-classes " " (:container size-classes) " " class)

        label-classes "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] select-none"

        trigger-change (fn [_]
                         (when on-change
                           (let [fake-event (js-obj)]
                             (on-change fake-event))))]

    [:div {:class container-classes}
     [:div.relative.flex.items-center.h-full
      [:input {:type "radio"
               :class radio-classes
               :checked checked
               :name name
               :value value
               :disabled disabled
               :on-change on-change}]

      (when checked
        [:div {:class (str "absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full "
                           (:dot size-classes) " " (:dot color-classes))
               :aria-hidden true}])]

     (when label
       [:label {:class (str "ml-2 " label-classes " cursor-pointer")
                :on-click trigger-change}
        label])]))