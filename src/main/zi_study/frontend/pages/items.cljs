(ns zi-study.frontend.pages.items
  (:require [reitit.frontend.easy :as rfe]))

(defn demo-item-page [match]
  (fn []
    (let [{:keys [path query]} (:parameters match)
          {:keys [id]} path]
      [:div.py-8.px-4
       [:div.card.max-w-lg.mx-auto.overflow-hidden
        [:div.card-header.flex.justify-between.items-center
         [:h1.text-xl.font-semibold
          [:span "Item "]
          [:span
           {:class "text-[var(--color-primary)]"}
           id]]
         [:span.text-white.py-1.px-3.text-sm.rounded-full
          {:class "bg-[var(--color-secondary)]"}
          "Details"]]
        [:div.card-body
         [:p.mb-4 (str "You are viewing item " id " with the following query parameters:")]
         [:pre.p-4.rounded-md.overflow-x-auto.text-sm
          {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"}
          (.stringify js/JSON (clj->js query) nil 2)]
         [:div.flex.justify-end.mt-6
          [:button.btn.btn-outlined
           {:on-click #(rfe/push-state ::frontpage)}
           "Back to Home"]]]]])))