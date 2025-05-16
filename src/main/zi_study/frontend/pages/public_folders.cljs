(ns zi-study.frontend.pages.public-folders
  (:require [reagent.core :as r]
            ["lucide-react" :as lucide-icons]))

(defn public-folders-page [match]
  [:div {:class "p-4 md:p-6 lg:p-8 animate-fade-in-up"}
   [:div {:class "flex items-center mb-6 pb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
    [:> lucide-icons/FolderGit2 {:size 32 :class "mr-3 text-[var(--color-primary)]"}]
    [:h1 {:class "text-3xl font-semibold tracking-tight text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
     "Public Folders"]]
   ;; Placeholder content
   [:p "Discover folders shared by other users. Feature coming soon!"]
   [:p "(Route Match data: " (pr-str match) ")"]])