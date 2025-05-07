(ns zi-study.frontend.components.button
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.utilities :refer [add-ripple-effect cx]]))

(defn button
  "A beautiful, customizable button component.
   
   Options:
   - variant: :primary, :secondary, :outlined, :text (default :primary)
   - size: :xs, :sm, :md, :lg, :xl (default :md)
   - full-width: true/false (default false)
   - start-icon: Lucide icon component to show before text
   - end-icon: Lucide icon component to show after text
   - disabled: true/false (default false)
   - loading: true/false (default false) - Shows a spinner and disables the button
   - class: additional CSS classes
   - on-click: function to call when clicked"
  [& args]
  (let [[opts children] (if (and (= 1 (count args)) (not (map? (first args))))
                          [{} args]  ; When called with just children, no props map
                          [(first args) (rest args)])  ; Normal case with props
        {:keys [variant size full-width start-icon end-icon disabled loading class on-click]
         :or {variant :primary
              size :md
              full-width false
              disabled false
              loading false}} opts

        is-disabled (or disabled loading)

        base-classes "relative overflow-hidden rounded-md font-medium inline-flex items-center justify-center transition-all duration-200 ease-in-out focus:outline-none"

        variant-classes (case variant
                          :primary "bg-[var(--color-primary)] hover:bg-[var(--color-primary-600)] active:bg-[var(--color-primary-700)] text-white focus:ring-[var(--color-primary-300)] dark:focus:ring-[var(--color-primary-400)]"
                          :secondary "bg-[var(--color-secondary)] hover:bg-[var(--color-secondary-dark)] active:bg-[var(--color-secondary-dark)] text-white focus:ring-[var(--color-secondary-light)]"
                          :outlined "border-2 border-[var(--color-primary)] text-[var(--color-primary)] hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)] dark:text-[var(--color-primary-300)] dark:border-[var(--color-primary-300)] focus:ring-[var(--color-primary-300)]"
                          :text "text-[var(--color-primary)] hover:bg-[var(--color-primary-50)] dark:text-[var(--color-primary-300)] dark:hover:bg-[rgba(233,30,99,0.15)] focus:ring-[var(--color-primary-300)]"
                          "bg-[var(--color-primary)] hover:bg-[var(--color-primary-600)] active:bg-[var(--color-primary-700)] text-white focus:ring-[var(--color-primary-300)]")

        size-classes (case size
                       :xs "text-xs py-1 px-2 gap-1"
                       :sm "text-sm py-1.5 px-3 gap-1.5"
                       :md "text-base py-2 px-4 gap-2"
                       :lg "text-lg py-2.5 px-5 gap-2.5"
                       :xl "text-xl py-3 px-6 gap-3"
                       "text-base py-2 px-4 gap-2")

        width-classes (if full-width "w-full" "")

        disabled-classes (if is-disabled
                           "opacity-50 cursor-not-allowed pointer-events-none"
                           "")

        all-classes (cx base-classes 
                        variant-classes 
                        size-classes 
                        width-classes 
                        disabled-classes 
                        class)

        handle-click (fn [e]
                       (when (and on-click (not is-disabled))
                         (on-click e))
                       (when-not is-disabled
                         (add-ripple-effect e)))

        attrs (-> (dissoc opts :variant :size :full-width :start-icon :end-icon :disabled :loading :class :on-click)
                  (assoc :class all-classes)
                  (assoc :on-click handle-click)
                  ;; Only add the disabled attribute if it's actually true
                  (cond-> is-disabled (assoc :disabled true)))]

    [:button attrs
     ;; Slot for the spinner, absolutely positioned on top
     (when loading
       [:div {:class "absolute inset-0 flex items-center justify-center z-10"} ; Added z-10 to ensure spinner is on top
        [:> lucide-icons/Loader {:class "animate-spin h-6 w-6"}]])

     ;; The actual content, always present for layout, but opacity changed when loading.
     ;; Content is not focusable or interactive if loading via disabled_classes on parent.
     [:span {:class (cx "inline-flex gap-1 items-center justify-center w-full"
                         (if loading "opacity-0" "opacity-100"))}
      (when start-icon
        [:div {:class "btn-icon flex-shrink-0"}
         [:> start-icon]])

      [:span {:class "min-w-0"} (into [:span] children)]

      (when end-icon
        [:div {:class "btn-icon flex-shrink-0"}
         [:> end-icon]])]]))

(defn button-group
  "A button group component for grouping related buttons.
   
   Options:
   - variant: :horizontal, :vertical (default :horizontal)
   - class: additional CSS classes"
  [{:keys [variant class] :or {variant :horizontal} :as opts} & children]
  (let [base-classes "inline-flex"

        variant-classes (case variant
                          :vertical "flex-col"
                          :horizontal "flex-row"
                          "flex-row")

        all-classes (cx base-classes variant-classes class)

        attrs (-> (dissoc opts :variant :class)
                  (assoc :class all-classes))]

    (into [:div attrs] children)))