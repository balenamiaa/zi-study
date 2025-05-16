(ns zi-study.frontend.pages.folder-details
  (:require [reagent.core :as r]
            ["lucide-react" :as lucide-icons]))

(defn folder-details-page [match]
  (let [folder-id (get-in match [:parameters :path :folder-id])]
    [:div {:class "p-4 md:p-6 lg:p-8 animate-fade-in-up"}
     [:div {:class "flex items-center mb-6 pb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
      [:> lucide-icons/FolderOpen {:size 32 :class "mr-3 text-[var(--color-primary)]"}]
      [:h1 {:class "text-3xl font-semibold tracking-tight text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
       (str "Folder Details (ID: " folder-id ")")]]
     ;; Placeholder content
     [:p "Details and question sets for this folder will be displayed here. Feature coming soon!"]
     [:p "(Route Match data: " (pr-str match) ")"]]))