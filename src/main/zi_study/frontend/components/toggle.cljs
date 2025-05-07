(ns zi-study.frontend.components.toggle
  (:require
   [reagent.core :as r]
   [zi-study.frontend.utilities :refer [cx]]))

(defn toggle
  "A beautiful toggle switch component for boolean selection.

   Options:
   - checked: true/false - current state of the toggle (required)
   - on-change: function to call when toggle is clicked (receives event)
   - label: text label for the toggle
   - label-position: :left, :right (default :right) - label position
   - disabled: true/false (default false) - disables the toggle
   - size: :sm, :md, :lg (default :md) - size of the toggle
   - color: :primary, :secondary, :success (default :primary) - color theme
   - class: additional CSS classes for the container
   - container-style: :default, :pill (default :default) - container style"
  [{:keys [checked on-change label label-position disabled size color class container-style]
    :or {label-position :right
         disabled false
         size :md
         color :primary
         container-style :default}}]

  (let [trigger-change (fn [e]
                         (when (and on-change (not disabled))
                           (.preventDefault e)
                           (.stopPropagation e)
                           (let [fake-event (js-obj)]
                             (on-change fake-event))))

        handle-keydown (fn [e]
                         (when (and (not disabled)
                                    (or (= (.-key e) " ")
                                        (= (.-key e) "Enter")))
                           (trigger-change e)))

        wrapper-classes (cx
                         (case container-style
                           :pill (cx "px-3 py-2 rounded-full border transition-all duration-200"
                                     (if checked
                                       "bg-[var(--color-primary-50)] dark:bg-[rgba(233,30,99,0.1)] border-[var(--color-primary-200)] dark:border-[var(--color-primary-400)]"
                                       "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"))
                           :default "")
                         "inline-flex items-center gap-2"
                         class)

        size-classes
        (case size
          :sm "scale-75"
          :md ""
          :lg "scale-125"
          "")

        color-classes
        (case color
          :primary "checked:from-primary-600 checked:to-primary-500 dark:checked:from-primary-500 dark:checked:to-primary-400"
          :secondary "checked:from-secondary checked:to-secondary-dark dark:checked:from-secondary-dark dark:checked:to-secondary"
          :success "checked:from-success-600 checked:to-success-500 dark:checked:from-success-500 dark:checked:to-success-400"
          "checked:from-primary-600 checked:to-primary-500 dark:checked:from-primary-500 dark:checked:to-primary-400")

        track-classes (cx "toggle-track"
                          color-classes
                          size-classes
                          (when disabled "opacity-50 cursor-not-allowed"))

        thumb-classes (cx "toggle-thumb"
                          (when disabled "opacity-50 cursor-not-allowed"))

        label-classes (cx "toggle-label select-none text-sm"
                          (cond
                            (and checked (not disabled)) "text-[var(--color-primary)] dark:text-[var(--color-primary-300)] font-medium"
                            disabled "opacity-50 cursor-not-allowed"))]

    [:div {:class wrapper-classes}
     ;; Label before toggle if position is :left
     (when (and label (= label-position :left))
       [:label {:class label-classes
                :on-click (when-not disabled trigger-change)}
        label])

     ;; Toggle switch
     [:div {:class track-classes
            :data-state (if checked "checked" "unchecked")
            :data-disabled (when disabled true)
            :on-click trigger-change
            :onKeyDown handle-keydown
            :aria-checked checked
            :aria-disabled disabled
            :role "switch"
            :tabIndex (when-not disabled 0)}
      [:div {:class thumb-classes
             :aria-hidden true}]]

     ;; Label after toggle if position is :right
     (when (and label (= label-position :right))
       [:label {:class label-classes
                :on-click (when-not disabled trigger-change)}
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

        checkbox-base "appearance-none transition-colors duration-200 ease-in-out focus:outline-none focus:ring-1 focus:ring-offset-1 disabled:opacity-50 disabled:cursor-not-allowed border-2"

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
                    :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-700)]"}
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
           :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-700)]"})

        checkbox-classes (cx checkbox-base
                             (:checkbox size-classes)
                             (:border color-classes)
                             (:checked color-classes)
                             (:ring color-classes)
                             "rounded bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
                             "checked:bg-no-repeat checked:bg-center"
                             input-class)

        container-classes (cx base-classes (:container size-classes) class)

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
       [:label {:class (cx "ml-2" label-classes "cursor-pointer")
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
                        "focus:outline-none focus:ring-1 focus:ring-offset-1 disabled:opacity-50 "
                        "disabled:cursor-not-allowed border-2")

        size-classes
        (case size
          :sm {:radio "w-4 h-4"
               :container "text-sm"}
          :md {:radio "w-5 h-5"
               :container "text-base"}
          :lg {:radio "w-6 h-6"
               :container "text-lg"}
          {:radio "w-5 h-5"
           :container "text-base"})

        color-classes
        (case color
          :primary {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                    :checked (str "checked:bg-gradient-to-r checked:from-primary-600 checked:to-primary-500 "
                                  "dark:checked:from-primary-500 dark:checked:to-primary-400 "
                                  "checked:border-primary-600 dark:checked:border-primary-400")
                    :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-700)]"}
          :secondary {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                      :checked (str "checked:bg-gradient-to-r checked:from-secondary checked:to-secondary-dark "
                                    "dark:checked:from-secondary-dark dark:checked:to-secondary "
                                    "checked:border-secondary dark:checked:border-secondary")
                      :ring "focus:ring-[var(--color-secondary-light)]"}
          :success {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                    :checked (str "checked:bg-gradient-to-r checked:from-success-600 checked:to-success-500 "
                                  "dark:checked:from-success-500 dark:checked:to-success-400 "
                                  "checked:border-success-600 dark:checked:border-success-400")
                    :ring "focus:ring-green-300"}
          {:border "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
           :checked (str "checked:bg-gradient-to-r checked:from-primary-600 checked:to-primary-500 "
                         "dark:checked:from-primary-500 dark:checked:to-primary-400 "
                         "checked:border-primary-600 dark:checked:border-primary-400")
           :ring "focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-700]"})

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
     [:input {:type "radio"
              :class radio-classes
              :checked checked
              :name name
              :value value
              :disabled disabled
              :on-change on-change}]

     (when label
       [:label {:class (str "ml-2 " label-classes " cursor-pointer")
                :on-click trigger-change}
        label])]))