(ns zi-study.frontend.layouts.main-layout
  (:require [zi-study.frontend.components.topbar :refer [topbar]]
            ["lucide-react" :as lucide-icons]))


(defn main-layout [props]
  (let [{:keys [children current-route auth-state]} props]
    [:div.min-h-screen.transition-colors.duration-300
     {:class "bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)]"}
     ;; Topbar
     [topbar {:current-route current-route 
              :auth-state auth-state}]

     ;; Main content with animation
     [:main.mt-6.animate-fade-in
      [:div.container.mx-auto.px-4
       children]]

     ;; Footer with nice gradient
     [:footer.mt-auto.py-8.border-t
      {:class "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
      [:div.container.mx-auto.px-4.flex.flex-col.md:flex-row.justify-between.items-center.gap-4
       [:div.flex.items-center
        [:div.mr-2.text-xl.font-bold.text-transparent.bg-clip-text
         {:class "bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
         "ZiStudy"]
        [:> lucide-icons/Sparkles {:size 16 :className "text-[var(--color-secondary)]"}]]

       [:div.flex.items-center.space-x-6
        [:a.nav-link {:href "#"}
         [:div.flex.items-center
          [:> lucide-icons/FileText {:size 16 :className "mr-1"}]
          "Documentation"]]
        [:a.nav-link {:href "#"}
         [:div.flex.items-center
          [:> lucide-icons/Github {:size 16 :className "mr-1"}]
          "GitHub"]]
        [:a.nav-link {:href "#"}
         [:div.flex.items-center
          [:> lucide-icons/LifeBuoy {:size 16 :className "mr-1"}]
          "Support"]]]]]]))