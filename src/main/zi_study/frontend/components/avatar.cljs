(ns zi-study.frontend.components.avatar
  (:require [zi-study.frontend.utilities :refer [cx]]))

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