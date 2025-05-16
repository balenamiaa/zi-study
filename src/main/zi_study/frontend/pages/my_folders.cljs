(ns zi-study.frontend.pages.my-folders
  (:require ["lucide-react" :as lucide-icons]))

(defn my-folders-page [match]
  [:div {:class "p-4 md:p-6 lg:p-8 animate-fade-in-up"}
   [:div {:class "flex items-center mb-6 pb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
    [:> lucide-icons/Folder {:size 32 :class "mr-3 text-[var(--color-primary)]"}]
    [:h1 {:class "text-3xl font-semibold tracking-tight text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
     "My Folders"]]
   [:p "This is where your personal folders will be listed. Feature coming soon!"]
   [:p "(Route Match data: " (pr-str match) ")"]])