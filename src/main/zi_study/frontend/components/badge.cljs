(ns zi-study.frontend.components.badge
  (:require
   [reagent.core :as r]
   [zi-study.frontend.utilities :refer [cx]]))

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
   - class: additional CSS classes"
  [{:keys [variant color size rounded count max dot position class]
    :or   {variant :filled
           color   :primary
           size    :sm
           rounded false
           dot     false}}
   & children]

  (let [base-classes "inline-flex items-center justify-center transition-all duration-200 ease-in-out"

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
          (case size
            :xs "text-[0.65rem] px-1 min-w-[18px] h-[18px]"
            :sm "text-xs px-1.5 min-w-[20px] h-[20px]"
            :md "text-sm px-2 min-w-[24px] h-[24px]"
            "text-xs px-1.5 min-w-[20px] h-[20px]"))

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

(defn- get-initials [text]
  (if (and text (pos? (count text)))
    (subs text 0 (min 2 (count text)))
    ""))

(defn avatar
  "An avatar component for displaying user profile pictures or initials.
   
   Options:
   - src: image URL
   - alt: image alt text
   - initials: text to show when no image (will use first 2 characters)
   - size: :xs, :sm, :md, :lg, :xl (default: :md)
   - variant: :rounded, :circle (default: :circle)
   - color: :primary, :secondary, :success, :warning, :error, :info (default: :primary)
   - status: :online, :offline, :busy, :away - adds a status indicator dot
   - class: additional CSS classes"
  [{:keys [src alt initials size variant color status class]
    :or   {size    :md
           variant :circle
           color   :primary}}]

  (let [base-classes "inline-flex items-center justify-center overflow-hidden transition-all duration-200 ease-in-out"

        size-classes
        (case size
          :xs "w-6 h-6 text-xs"
          :sm "w-8 h-8 text-sm"
          :md "w-10 h-10 text-base"
          :lg "w-12 h-12 text-lg"
          :xl "w-16 h-16 text-xl"
          "w-10 h-10 text-base")

        variant-classes
        (case variant
          :rounded "rounded-md"
          :circle  "rounded-full"
          "rounded-full")

        color-classes
        (if src
          ""
          (case color
            :primary   "bg-gradient-to-br from-[var(--color-primary-300)] to-[var(--color-primary-700)] text-white"
            :secondary "bg-gradient-to-br from-[var(--color-secondary-light)] to-[var(--color-secondary-dark)] text-white"
            :success   "bg-gradient-to-br from-green-300 to-green-700 text-white"
            :warning   "bg-gradient-to-br from-orange-300 to-orange-700 text-white"
            :error     "bg-gradient-to-br from-red-300 to-red-700 text-white"
            :info      "bg-gradient-to-br from-blue-300 to-blue-700 text-white"
            "bg-gradient-to-br from-[var(--color-primary-300)] to-[var(--color-primary-700)] text-white"))

        all-classes (cx base-classes size-classes variant-classes color-classes class)

        status-dot-color
        (case status
          :online  "bg-[var(--color-success)]"
          :offline "bg-[var(--color-light-text-secondary)] dark:bg-[var(--color-dark-text-secondary)]"
          :busy    "bg-[var(--color-error)]"
          :away    "bg-[var(--color-warning)]"
          "")]

    [:div {:class "relative inline-block"}
     [:div {:class all-classes}
      (if src
        [:img {:class "w-full h-full object-cover"
               :src src
               :alt (or alt initials "Avatar")}]
        [:div {:class "flex items-center justify-center w-full h-full font-medium uppercase"}
         (get-initials initials)])]

     ;; Status indicator
     (when status
       [:span {:class (cx "absolute bottom-0 right-0 transform border-2 border-white rounded-full translate-x-1/2 translate-y-1/2 w-1/4 h-1/4 min-w-[8px] min-h-[8px]" status-dot-color)}])]))