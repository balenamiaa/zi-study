(ns zi-study.frontend.layouts.main-layout
  (:require [zi-study.frontend.components.topbar :refer [topbar]]
            ["lucide-react" :as lucide-icons]))


(defn main-layout [props]
  (let [{:keys [children current-route]} props]
    [:div {:class "min-h-screen bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)]"}
     ;; Topbar
     [topbar {:current-route current-route}]

     ;; Main content with animation - adjust top margin to accommodate floating pill navbar
     [:main {:class "mt-20 pt-8 animate-fade-in"}
      [:div {:class "container mx-auto p-2"}
       children]]

     ;; Footer with nice gradient
     [:footer {:class "mt-auto py-4 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
      [:div {:class "container mx-auto px-4 flex flex-col md:flex-row justify-between items-center gap-2"}
       [:div {:class "flex items-center"}
        [:div {:class "mr-2 text-xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
         "ZiStudy"]
        [:> lucide-icons/Sparkles {:size 16 :className "text-[var(--color-secondary)]"}]]

       [:div {:class "flex items-center space-x-6 scale-150"}
        [:div {:class "flex items-center"}
         "Made with "
         [:> lucide-icons/Heart {:size 16
                                 :className "mx-1 text-pink-500 animate-pulse"
                                 :fill "currentColor"}]
         " for darling"]]]]]))