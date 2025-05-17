(ns zi-study.frontend.pages.public-folders
  (:require
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   ["lucide-react" :as lucide-icons]
   [clojure.string :as str]
   [zi-study.frontend.state :as state]
   [zi-study.frontend.utilities.http :as http]
   [zi-study.frontend.components.button :refer [button]]
   [zi-study.frontend.components.card :refer [card card-content card-footer card-header]]
   [zi-study.frontend.components.skeleton :refer [skeleton-text]]
   [zi-study.frontend.components.input :refer [text-input]]
   [zi-study.frontend.components.pagination :refer [pagination]]
   [zi-study.frontend.components.folder-display-card :refer [folder-display-card]]
   [zi-study.frontend.utilities :refer [cx]]))

(defn folder-filters [{:keys [search-term on-search-change]}]
  [:div {:class "flex flex-col md:flex-row gap-4 mb-6"}
   [:div {:class "w-full md:w-80"}
    [text-input {:placeholder "Search folders..."
                 :value search-term
                 :start-icon lucide-icons/Search
                 :on-change #(on-search-change (.. % -target -value))}]]])

(defn folder-grid [{:keys [folders loading?]}]
  [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-6"}
   (cond
     loading?
     (for [i (range 6)]
       ^{:key (str "skeleton-" i)}
       [card {:class "h-64"}
        [card-header {:title [skeleton-text {:width "70%"}]
                      :subtitle [skeleton-text {:width "30%"}]}]
        [card-content
         [:div {:class "space-y-2"}
          [skeleton-text {:width "100%"}]
          [skeleton-text {:width "100%"}]
          [skeleton-text {:width "60%"}]]]])

     (empty? folders)
     [:div {:class "col-span-full flex flex-col items-center justify-center py-12 text-center"}
      [:> lucide-icons/FolderOpen {:size 64 :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mb-4 opacity-50"}]
      [:h3 {:class "text-xl font-medium mb-2"} "No public folders found"]
      [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] max-w-md"}
       "Try adjusting your search criteria or check back later."]]

     :else
     (for [folder folders]
       ^{:key (:folder-id folder)}
       [folder-display-card {:folder folder :show-privacy-badge? false}]))])

(defn public-folders-page []
  (let [search-term (r/atom "")
        folders-state (state/get-public-folders-list-state)

        load-folders (fn [params]
                       (http/get-public-folders
                        (merge
                         {:search @search-term}
                         params)
                        nil))

        handle-page-change (fn [page]
                             (state/set-public-folders-page page)
                             (load-folders {:page page}))]

    (r/create-class
     {:component-did-mount
      (fn [_]
        (load-folders {}))

      :reagent-render
      (fn []
        (let [{:keys [list loading? error pagination]} @folders-state
              {:keys [page limit total_items total_pages]} pagination]
          [:div {:class "container max-w-7xl mx-auto px-4 py-8"}
           ;; Header section
           [:div {:class "mb-8"}
            [:h1 {:class "text-2xl sm:text-3xl font-bold mb-2"} "Public Folders"]
            [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
             "Browse and explore question sets shared by other users"]]

           ;; Filters
           [folder-filters {:search-term @search-term
                            :on-search-change (fn [value]
                                                (reset! search-term value)
                                                ;; Debounce search
                                                (js/clearTimeout (.-searchTimer js/window))
                                                (set! (.-searchTimer js/window)
                                                      (js/setTimeout
                                                       #(load-folders {:page 1 :search value})
                                                       300)))}]

           ;; Error message if any
           (when error
             [:div {:class "bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 rounded-lg p-4 mb-6"}
              [:div {:class "flex items-start"}
               [:> lucide-icons/AlertCircle {:className "flex-shrink-0 mr-2 h-5 w-5"}]
               [:div
                [:p {:class "font-medium"} "Failed to load public folders"]
                [:p {:class "text-sm mt-1"} error]]]])

           ;; Folders grid
           [folder-grid {:folders list :loading? loading?}]

           ;; Pagination
           (when (and (not loading?) (> total_pages 1))
             [pagination {:page page
                          :total-pages total_pages
                          :total-items total_items
                          :limit limit
                          :on-page-change handle-page-change
                          :item-name "folders"}])]))})))