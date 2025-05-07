(ns zi-study.frontend.pages.question-sets
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.input :refer [text-input]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.badge :refer [badge]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.components.skeleton :as skeleton]
            ["lucide-react" :as lucide-icons]))

;; --- Helper Functions ---
(defn format-date [date-str]
  (-> date-str js/Date. .toLocaleDateString))

;; --- Tag Filter Component ---
(defn tag-filter [{:keys [tags selected-tags on-tag-click]}]
  [:div {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl p-4 shadow-sm"}
   [:div {:class "flex items-center justify-center gap-2 mb-4 pb-2 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
    [:> lucide-icons/Tags {:size 22
                           :className "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"}]
    [:h4 {:class "text-lg font-medium bg-gradient-to-r from-[var(--color-primary-700)] to-[var(--color-primary-500)] bg-clip-text text-transparent dark:from-[var(--color-primary-300)] dark:to-[var(--color-primary-200)]"}
     "Filter by Tags"]]

   [:div {:class "flex flex-wrap gap-2 justify-center"}
    (map-indexed
     (fn [idx tag]
       ^{:key idx}
       [:div {:class (str "text-sm select-none inline-flex items-center justify-center px-2 py-1 rounded-full cursor-pointer "
                          (if (contains? selected-tags tag)
                            "bg-[var(--color-secondary)] text-white"
                            "bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg-paper)] text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] border-2 border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] hover:border-[var(--color-secondary-light)] dark:hover:border-[var(--color-secondary-light)]"))
              :on-click #(on-tag-click tag)}
        (if (contains? selected-tags tag)
          [:div {:class "flex items-center"}
           [:> lucide-icons/Check {:size 16 :className "mr-1.5"}]
           [:span {:class "font-medium"} tag]]
          [:div {:class "flex items-center"}
           [:> lucide-icons/Plus {:size 16 :className "mr-1.5"}]
           [:span tag]])])
     tags)]])

;; --- Search Bar Component ---
(defn search-bar [{:keys [value on-change on-change-debounced]}]
  [:div {:class "relative w-full"}
   [text-input
    {:placeholder "Search question sets..."
     :value value
     :start-icon lucide-icons/Search
     :on-change on-change
     :on-change-debounced on-change-debounced
     :class "w-full"}]])

;; --- Question Set Card Component ---
(defn question-set-card [{:keys [set-id title description created-at total-questions tags]}]
  (let [selected-tags (:tags @(state/get-sets-filters))]
    [card {:hover-effect true
           :class "h-full animate-fade-in-up transition-all duration-300 transform hover:scale-[1.02]"
           :on-click #(rfe/push-state :zi-study.frontend.core/set-page {:set-id set-id})}
     [:div {:class "p-6"}
      [:div {:class "flex items-start justify-between gap-4 mb-4"}
       [:div
        [:h3 {:class "text-lg font-semibold text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)] mb-2"}
         title]
        [:p {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] line-clamp-2"}
         description]]
       [:div {:class "flex-shrink-0 bg-[var(--color-primary-50)] dark:bg-[rgba(var(--color-primary-rgb),0.1)] rounded-lg p-3"}
        [:div {:class "text-center"}
         [:div {:class "text-xl font-bold text-[var(--color-primary)]"} total-questions]
         [:div {:class "text-xs text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Questions"]]]]

      [:div {:class "space-y-4"}
       [:div {:class "flex flex-wrap gap-1"}
        (map-indexed
         (fn [idx tag]
           ^{:key idx}
           [badge {:variant (if (contains? selected-tags tag) :solid :soft)
                   :size :sm
                   :color (if (contains? selected-tags tag) :secondary :default)}
            tag])
         tags)]

       [:div {:class "flex items-center justify-between text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
        [:div {:class "flex items-center gap-1"}
         [:> lucide-icons/Calendar {:size 14}]
         (format-date created-at)]]]]]))

;; --- Skeleton for Question Set Card ---
(defn question-set-card-skeleton []
  [skeleton/skeleton-card
   {:header true
    :media false ; No big media image in the actual card, so media skeleton not needed
    :content-rows 2
    :footer true}])

;; --- Empty State Component ---
(defn empty-state [{:keys [on-reset]}]
  [:div {:class "text-center py-12 border border-dashed rounded-xl border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"}
   [:> lucide-icons/Search {:size 48 :className "mx-auto mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]
   [:h3 {:class "text-xl font-medium mb-2 text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
    "No question sets found"]
   [:p {:class "mb-6 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
    "Try adjusting your filters or search terms"]
   [button
    {:variant :outlined
     :start-icon lucide-icons/RefreshCw
     :on-click on-reset}
    "Clear filters"]])

;; --- Pagination Component ---
(defn pagination [{:keys [page total-pages total-items limit on-page-change]}]
  [:div {:class "flex flex-col sm:flex-row justify-center items-center gap-4 mt-8"}
   [:div {:class "flex justify-center items-center gap-2"}
    [button
     {:variant :outlined
      :size :sm
      :disabled (= page 1)
      :on-click #(on-page-change (dec page))
      :start-icon lucide-icons/ChevronLeft}
     "Previous"]

    [:div {:class "flex gap-1"}
     (for [p (cond
               (<= total-pages 5) (range 1 (inc total-pages))
               (< page 3) (range 1 6)
               (> page (- total-pages 2)) (range (- total-pages 4) (inc total-pages))
               :else (range (- page 2) (+ page 3)))]
       ^{:key p}
       [button
        {:variant (if (= p page) :primary :text)
         :size :sm
         :on-click #(on-page-change p)}
        (str p)])]

    [button
     {:variant :outlined
      :size :sm
      :disabled (= page total-pages)
      :on-click #(on-page-change (inc page))
      :end-icon lucide-icons/ChevronRight}
     "Next"]]

   [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
    (str "Showing " (if (zero? total-items) 0 (inc (* (dec page) limit))) "-" (min (* page limit) total-items) " of " total-items)]])

;; --- Main Page Component ---
(defn question-sets-page []
  (r/create-class
   {:component-did-mount
    (fn []
      (let [sets-state @(state/get-sets-list-state)
            pagination (assoc (:pagination sets-state) :page 1) ; Always fetch page 1 on mount
            filters (:filters sets-state)]
        (http/get-tags (fn [_] nil))
        (http/get-sets filters pagination (fn [_] nil))))

    :reagent-render
    (fn []
      (let [sets-state @(state/get-sets-list-state)
            tags-state @(state/get-tags-state)
            sets-loading? (:loading? sets-state)
            tags-loading? (:loading? tags-state)
            error (:error sets-state)
            question-sets (:list sets-state)
            pagination (:pagination sets-state)
            filters (:filters sets-state)
            selected-tags (:tags filters)]

        [:div {:class "container mx-auto px-4 py-8 max-w-7xl"}
         [:div {:class "mb-8"}
          [:div {:class "flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6"}
           [:h1 {:class "text-2xl font-bold text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
            "Question Sets"]
           (when (and (not sets-loading?) (pos? (get-in sets-state [:pagination :total_items] 0)))
             [badge
              {:color :primary
               :variant :outlined
               :size :lg
               :class "text-xl p-3"}
              (str (get-in sets-state [:pagination :total_items]) " sets")])]

          ;; Search and filters section
          [:div {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl p-6 shadow-sm"}
           [:div {:class "flex flex-col md:flex-row gap-6 items-center"}
            [:div {:class "flex-grow w-full md:w-auto"}
             [search-bar
              {:value (:search filters)
               :on-change #(let [new-search-term (.. % -target -value)]
                             (state/set-sets-filter-search new-search-term))
               :on-change-debounced {:time 300
                                     :callback (fn [event]
                                                 (let [current-filters @(state/get-sets-filters)
                                                       current-pagination (:pagination @(state/get-sets-list-state))
                                                       search-val (.. event -target -value)]
                                                   (http/get-sets (assoc current-filters :search search-val)
                                                                  (assoc current-pagination :page 1)
                                                                  (fn [_] nil))))}}]]
            [:div {:class "flex-shrink-0"}
             [button
              {:variant :outlined
               :start-icon lucide-icons/RefreshCw
               :disabled sets-loading?
               :on-click #(do
                            (state/set-sets-filter-tags #{})
                            (state/set-sets-filter-search "")
                            (http/get-sets {:tags #{} :search ""} (assoc pagination :page 1) (fn [_] nil)))}
              "Reset All"]]]

           (cond
             tags-loading?
             [:div {:class "mt-4 p-4"}
              [skeleton/skeleton-text {:rows 2 :variant-width true}]]

             (seq (:list tags-state))
             [:div {:class "mt-4"}
              [tag-filter
               {:tags (:list tags-state)
                :selected-tags selected-tags
                :on-tag-click #(let [new-tags (if (contains? selected-tags %)
                                                (disj selected-tags %)
                                                (conj selected-tags %))]
                                 (state/set-sets-filter-tags new-tags)
                                 (http/get-sets (assoc filters :tags new-tags)
                                                (assoc pagination :page 1)
                                                (fn [_] nil)))}]]
             :else
             [:div {:class "mt-4 p-4 text-center text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
              "No tags available."])]]

         ;; Error message
         (when (and error (not sets-loading?))
           [alert
            {:variant :soft
             :color :error
             :dismissible true
             :on-dismiss #(state/set-sets-error nil)
             :class "mb-6"}
            error])

         ;; Loading state for question sets
         (when sets-loading?
           [:div
            [:div {:class "grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6 mt-6"}
             (for [_ (range (get-in pagination [:limit] 3))] ; Show a few skeleton cards based on limit or a default
               ^{:key (gensym "skeleton-set-")}
               [question-set-card-skeleton])]])
         ;; Content when not loading
         (when (and (empty? question-sets) (not error)) ; Show empty state only if no error and not loading
           [:div {:class "mt-6"}
            [empty-state
             {:on-reset #(do
                           (state/set-sets-filter-tags #{})
                           (state/set-sets-filter-search "")
                           (http/get-sets {:tags #{} :search ""} (assoc pagination :page 1) (fn [_] nil)))}]])
         ;; Question sets grid
         (when (seq question-sets)
           [:div
            [:div {:class "grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6 mt-6"}
             (for [set question-sets]
               ^{:key (:set-id set)}
               [question-set-card set])]

            (when (and (:total_pages pagination) (> (:total_pages pagination) 1))
              [pagination
               {:page (:page pagination)
                :total-pages (:total_pages pagination)
                :total-items (:total_items pagination)
                :limit (:limit pagination)
                :on-page-change #(do
                                   (state/set-sets-page %)
                                   (http/get-sets filters (assoc pagination :page %) (fn [_] nil)))}])])]))}))