(ns zi-study.frontend.layouts.active-learning-layout
  (:require [reagent.core :as r]
            [zi-study.frontend.components.active-learning-sidebar :refer [active-learning-sidebar]]
            [zi-study.frontend.components.topbar :refer [topbar]]
            [zi-study.frontend.components.flashgroup :refer [flashgroup]]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.routes :as routes]
            ["lucide-react" :as lucide-icons]))

(defn active-learning-layout [props]
  (let [{:keys [children current-route active-learning-page]} props
        mobile-sidebar-open? (r/atom false)
        desktop-sidebar-expanded? (r/atom true)
        toggle-mobile-sidebar #(swap! mobile-sidebar-open? not)
        handle-desktop-sidebar-toggle (fn [is-expanded]
                                        (reset! desktop-sidebar-expanded? is-expanded))
        ;; Define sidebar links here, so they can be customized per layout
        sidebar-links [{:id :question-sets
                        :href (rfe/href routes/sym-active-learning-question-sets-route)
                        :text "Question Sets"
                        :icon lucide-icons/LayoutGrid}
                       {:id :my-folders
                        :href (rfe/href routes/sym-my-folders-route)
                        :text "My Folders"
                        :icon lucide-icons/Folder}
                       {:id :public-folders
                        :href (rfe/href routes/sym-public-folders-route)
                        :text "Public Folders"
                        :icon lucide-icons/FolderGit2}
                       {:id :advanced-search
                        :href (rfe/href routes/sym-advanced-search-route)
                        :text "Search All Questions"
                        :icon lucide-icons/SearchCode}]]

    [:div {:class "min-h-screen flex flex-col bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)]"}
     [topbar {:current-route current-route}]

     [flashgroup {:default-position :top-right}]

     [:div {:class "flex flex-1 mt-[calc(theme(spacing.2)+3rem)]"}
      [active-learning-sidebar {:current-page active-learning-page
                                :links sidebar-links
                                :mobile-open? mobile-sidebar-open?
                                :toggle-mobile-sidebar toggle-mobile-sidebar
                                :on-desktop-toggle handle-desktop-sidebar-toggle}]

      [:main {:class "flex-1 p-4 md:p-6 lg:p-8 overflow-y-auto scrollable-content transition-all duration-300 ease-in-out"}
       children]]]))