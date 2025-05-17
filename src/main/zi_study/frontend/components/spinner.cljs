(ns zi-study.frontend.components.spinner
  (:require [zi-study.frontend.utilities :refer [cx]]))

(defn spinner [{:keys [size color class]
                :or {size :md ; :xs, :sm, :md, :lg, :xl
                     color :primary ; :primary, :secondary, :current (uses text color)
                     }}]
  (let [size-classes {:xs "w-3 h-3"
                      :sm "w-4 h-4"
                      :md "w-6 h-6"
                      :lg "w-8 h-8"
                      :xl "w-12 h-12"}
        color-classes (case color
                        :primary "border-[var(--color-primary)]"
                        :secondary "border-[var(--color-light-text-secondary)] dark:border-[var(--color-dark-text-secondary)]"
                        :current "border-current"
                        "border-current" ; Default to current text color
                        )]
    [:div {:class (cx "animate-spin rounded-full border-t-2 border-b-2"
                      (get size-classes size "w-6 h-6")
                      color-classes
                      class)
           :role "status"}
     [:span {:class "sr-only"} "Loading..."]]))
