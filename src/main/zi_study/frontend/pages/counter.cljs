(ns zi-study.frontend.pages.counter
  (:require [reagent.core :as r]
            ["lucide-react" :as lucide-icons]))

(defonce counter-count (r/atom 0))

(defn counter-page []
  (fn []
    [:div.flex.flex-col.items-center.justify-center.py-12
     [:div.card.max-w-md.w-full.p-8
      [:h1.text-2xl.font-semibold.mb-6.text-center "Interactive Counter"]
      [:div.text-center.mb-8
       [:div.text-5xl.font-bold.mb-3
        {:class "text-[var(--color-primary)]"}
        @counter-count]
       [:p
        {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
        "Click the buttons below to change the count"]]
      [:div.flex.justify-center.gap-4
       [:button
        {:class "btn btn-secondary flex items-center" :on-click #(swap! counter-count dec)}
        [:> lucide-icons/Minus {:size 18}]
        [:span.ml-1 "Decrement"]]
       [:button
        {:class "btn btn-primary flex items-center" :on-click #(swap! counter-count inc)}
        [:> lucide-icons/Plus {:size 18}]
        [:span.ml-1 "Increment"]]]]]))