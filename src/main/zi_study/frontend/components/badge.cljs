(ns zi-study.frontend.components.badge
  (:require [zi-study.frontend.utilities :refer [cx]]))

(defn badge
  "A badge component for showing counts, notifications, or status indicators.
   
   Options:
   - variant: :filled, :outlined (default: :filled)
   - color: :primary, :secondary, :success, :warning, :error, :info (default: :primary)
   - size: :xs, :sm, :md (default: :sm)
   - rounded: true/false (default: false) - fully rounded or slightly rounded corners
   - count: number - display a number in the badge
   - max: number - max value to show before displaying '+' (e.g. '5+')
   - dot: true/false (default: false) - shows a simple dot instead of content
   - position: :top-right, :top-left, :bottom-right, :bottom-left - position when used as an overlay
   - auto-height: true/false (default: false) - allows height to adjust to content
   - class: additional CSS classes"
  [{:keys [variant color size rounded count max dot position auto-height class]
    :or   {variant :filled
           color   :primary
           size    :sm
           rounded false
           dot     false
           auto-height false}}
   & children]

  (let [base-classes "inline-flex items-center justify-center align-middle transition-all duration-200 ease-in-out leading-none"

        color-classes
        (case [color variant]
          [:primary :filled]     "bg-[var(--color-primary)] text-white"
          [:primary :outlined]   "bg-transparent text-[var(--color-primary)] border border-[var(--color-primary)] dark:text-[var(--color-primary-300)] dark:border-[var(--color-primary-300)]"
          [:secondary :filled]   "bg-[var(--color-secondary)] text-white"
          [:secondary :outlined] "bg-transparent text-[var(--color-secondary)] border border-[var(--color-secondary)]"
          [:success :filled]     "bg-[var(--color-success)] text-white"
          [:success :outlined]   "bg-transparent text-[var(--color-success)] border border-[var(--color-success)]"
          [:warning :filled]     "bg-[var(--color-warning)] text-white"
          [:warning :outlined]   "bg-transparent text-[var(--color-warning)] border border-[var(--color-warning)]"
          [:error :filled]       "bg-[var(--color-error)] text-white"
          [:error :outlined]     "bg-transparent text-[var(--color-error)] border border-[var(--color-error)]"
          [:info :filled]        "bg-[var(--color-info)] text-white"
          [:info :outlined]      "bg-transparent text-[var(--color-info)] border border-[var(--color-info)]"
          "bg-[var(--color-primary)] text-white")

        size-classes
        (if dot
          (case size
            :xs "w-1.5 h-1.5"
            :sm "w-2 h-2"
            :md "w-2.5 h-2.5"
            "w-2 h-2")
          (if auto-height
            ;; Auto height classes - height adjusts to content
            (case size
              :xs "text-[0.65rem] px-1 min-w-[18px] py-1 flex items-center"
              :sm "text-xs px-1.5 min-w-[20px] py-1 flex items-center"
              :md "text-sm px-2 min-w-[24px] py-1.5 flex items-center"
              :lg "text-base px-3 min-w-[28px] py-2 flex items-center"
              "text-xs px-1.5 min-w-[20px] py-1 flex items-center")
            ;; Fixed height classes
            (case size
              :xs "text-[0.65rem] px-1 min-w-[18px] h-[18px] flex items-center"
              :sm "text-xs px-1.5 min-w-[20px] h-[20px] flex items-center"
              :md "text-sm px-2 min-w-[24px] h-[24px] flex items-center"
              :lg "text-base px-3 min-w-[28px] h-[28px] flex items-center"
              "text-xs px-1.5 min-w-[20px] h-[20px] flex items-center")))

        rounded-classes (if (or rounded dot) "rounded-full" "rounded")

        position-classes
        (case position
          :top-right    "absolute top-0 right-0 -translate-y-1/2 translate-x-1/2"
          :top-left     "absolute top-0 left-0 -translate-y-1/2 -translate-x-1/2"
          :bottom-right "absolute bottom-0 right-0 translate-y-1/2 translate-x-1/2"
          :bottom-left  "absolute bottom-0 left-0 translate-y-1/2 -translate-x-1/2"
          "")

        all-classes (cx base-classes color-classes size-classes rounded-classes position-classes class)

        content
        (cond
          dot                []
          count             [(str (if (and max (> count max))
                                    (str max "+")
                                    count))]
          (seq children)    children
          :else             [])]

    (into [:div {:class all-classes}] content)))

