(ns zi-study.frontend.layouts.main-layout
  (:require [zi-study.frontend.components.topbar :refer [topbar]]
            [zi-study.frontend.components.flashgroup :refer [flashgroup]]
            ["lucide-react" :as lucide-icons]))


(defn main-layout [props]
  (let [{:keys [children current-route]} props]
    [:div {:class "min-h-screen bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)]"}
     ;; Topbar
     [topbar {:current-route current-route}]

     ;; Flash messages
     [flashgroup {:default-position :top-right}]

     ;; Main content with animation - adjust top margin to accommodate floating pill navbar
     [:main {:class "mt-20 pt-8 animate-fade-in scrollable-content"}
      [:div {:class "container mx-auto p-2"}
       children]]

     ;; Footer with nice gradient
     [:footer {:class "mt-auto py-2 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
      [:div {:class "flex justify-end items-center px-2 text-2xl"}
       [:div {:class "flex items-center"}
        "Made with "
        [:> lucide-icons/Heart {:size 24
                                :className "mx-1 text-pink-500 animate-pulse"
                                :fill "currentColor"}]
        " for darling"]]]]))