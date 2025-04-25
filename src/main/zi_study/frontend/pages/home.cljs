(ns zi-study.frontend.pages.home
  (:require
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.state :refer [app-state]]))

(defn home-page []
  (fn []
    [:div.py-8.px-4
     [:h1.text-3xl.font-bold.mb-6.text-transparent.bg-clip-text
      {:class "bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
      "Welcome to ZiStudy"]
     [:p.mb-6
      {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
      "A beautiful learning platform designed with modern aesthetics."]
     [:div.flex.flex-wrap.gap-4
      [:button
       {:class "btn btn-primary flex items-center gap-2"
        :on-click #(swap! app-state assoc :text "Button clicked!")}
       [:> lucide-icons/BookOpen {:size 20}]
       [:span "Start Learning"]]
      [:button
       {:class "btn btn-outlined flex items-center gap-2"
        :on-click #(js/console.log "Explore clicked")}
       [:> lucide-icons/Compass {:size 20}]
       [:span "Explore"]]]]))