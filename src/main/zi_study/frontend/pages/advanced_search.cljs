(ns zi-study.frontend.pages.advanced-search
  (:require [reagent.core :as r]
            ["lucide-react" :as lucide-icons]))

(defn advanced-search-page [match]
  [:div {:class "p-4 md:p-6 lg:p-8 animate-fade-in-up"}
   [:div {:class "flex items-center mb-6 pb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
    [:> lucide-icons/SearchCode {:size 32 :class "mr-3 text-[var(--color-primary)]"}]
    [:h1 {:class "text-3xl font-semibold tracking-tight text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
     "Advanced Search"]]

   [:div {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] p-10 rounded-2xl shadow-xl min-h-[300px] flex flex-col items-center justify-center"}
    [:> lucide-icons/Construction {:size 64 :class "mb-6 text-[var(--color-secondary)] animate-pulse"}]
    [:h2 {:class "text-xl font-medium mb-2 text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
     "Coming Soon!"]
    [:p {:class "text-md text-center text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] max-w-md"}
     "This section is under construction. Get ready for powerful tools to pinpoint the exact questions you need."]]

   [:div {:class "mt-8"}
    [:h3 {:class "text-xl font-semibold mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"} "Search Parameters (Example)"]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-6"}
     [:div {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] p-6 rounded-lg shadow-md"}
      [:p "Keyword search, tag filters, difficulty sliders, etc."]]
     [:div {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] p-6 rounded-lg shadow-md"}
      [:p "Date range selectors, source material filters, etc."]]]]])