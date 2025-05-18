(ns zi-study.frontend.pages.question-sets
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.routes :as routes]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.input :refer [text-input]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.badge :refer [badge]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.components.skeleton :refer [skeleton-card skeleton-text]]
            [zi-study.frontend.components.pagination :refer [pagination]]
            ["lucide-react" :as lucide-icons]))

;; --- Helper Functions ---
(defn format-date [date-str]
  (-> date-str js/Date. .toLocaleDateString))

(defn format-percentage [value]
  (str (int (* value 100)) "%"))

;; --- Radial Progress Chart Component ---
(defn radial-progress-chart [{:keys [total answered correct size]
                              :or {size 80}}]
  (let [radius (/ size 2)
        stroke-width 4
        circle-radius (- radius (/ stroke-width 2.75))
        circumference (* circle-radius 2 Math/PI)

        answered-ratio (if (and (some? total) (pos? total) (some? answered) (>= answered 0))
                         (min 1.0 (/ answered total))
                         0.0)
        correct-ratio  (if (and (some? total) (pos? total) (some? correct) (>= correct 0))
                         (min 1.0 (/ correct total))
                         0.0)

        final-correct-arc-ratio correct-ratio

        answered-arc-length (* circumference answered-ratio)
        correct-arc-length (* circumference final-correct-arc-ratio)

        center radius
        text-size (/ size 5)

        percent-correct-of-answered (if (and (some? answered) (pos? answered) (some? correct))
                                      (min 1.0 (max 0.0 (/ correct answered)))
                                      0.0)]
    [:div {:class "relative" :style {:width size :height size}}
     [:svg {:width size :height size :viewBox (str "0 0 " size " " size)}
      ;; Background circle
      [:circle {:cx center :cy center :r circle-radius
                :stroke "var(--color-light-divider)" :stroke-width stroke-width
                :fill "none" :class "dark:stroke-[var(--color-dark-divider)]"}]

      ;; Answered progress (e.g., Purple) - Base layer for answered questions
      [:circle {:cx center :cy center :r circle-radius
                :stroke "var(--color-primary-300)" :stroke-width stroke-width
                :fill "none"
                :stroke-dasharray (str answered-arc-length " " circumference)
                :stroke-dashoffset 0 :transform (str "rotate(-90 " center " " center ")")
                :class "transition-all duration-500 ease-out"}]

      ;; Correct progress (e.g., Pink/Green) - Overlay showing correct portion of total
      [:circle {:cx center :cy center :r circle-radius
                :stroke "var(--color-secondary)" :stroke-width stroke-width
                :fill "none"
                :stroke-dasharray (str correct-arc-length " " circumference)
                :stroke-dashoffset 0 :transform (str "rotate(-90 " center " " center ")")
                :class "transition-all duration-500 ease-out"}]]

     [:div {:class "absolute inset-0 flex flex-col items-center justify-center"}
      [:div {:class "text-base font-bold text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"
             :style {:font-size text-size}}
       (str (or answered 0) "/" (or total 0))]
      [:div {:class "text-xs text-[var(--color-secondary)] font-bold flex items-center justify-center gap-0.5"}
       (cond
         (not (and (some? total) (pos? total))) [:span "No questions"]
         (not (and (some? answered) (pos? answered))) [:span "Not started"]
         :else [:<>
                [:span (format-percentage percent-correct-of-answered)]
                [:> lucide-icons/Check {:size 10 :strokeWidth 3}]])]]]))

;; --- Tag Filter Component ---
(defn tag-filter [{:keys [tags selected-tags on-tag-click]}]
  [:div {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl p-4 shadow-sm"}
   [:div {:class "flex items-center justify-center gap-2 mb-4 pb-2 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
    [:> lucide-icons/Tags {:size 22
                           :className "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"}]
    [:h4 {:class "text-lg font-medium bg-gradient-to-r from-[var(--color-primary-700)] to-[var(--color-primary-500)] bg-clip-text text-transparent dark:from-[var(--color-primary-300)] dark:to-[var(--color-primary-200)]"}
     "Filter by Tags"]]

   [:div {:class "flex flex-wrap gap-2 justify-center max-h-[180px] overflow-y-auto pr-1 pb-1 scrollbar-thin scrollbar-thumb-[var(--color-light-divider)] dark:scrollbar-thumb-[var(--color-dark-divider)] scrollbar-track-transparent"}
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
(defn question-set-card [{:keys [set-id title description created-at total-questions tags progress]}]
  (let [selected-tags (:tags @(state/get-sets-filters))
        answered (get-in progress [:answered] 0)
        correct (get-in progress [:correct] 0)]
    [card {:hover-effect true
           :class "h-full animate-fade-in-up transition-all duration-300 transform hover:scale-[1.02]"
           :on-click #(rfe/push-state routes/sym-set-page-route {:set-id set-id})}
     [:div {:class "p-6"}
      [:div {:class "flex items-start justify-between gap-4 mb-4"}
       [:div
        [:h3 {:class "text-lg font-semibold text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)] mb-2"}
         title]
        [:p {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] line-clamp-2"}
         description]]

       ;; Progress chart instead of just numbers
       [radial-progress-chart
        {:total total-questions
         :answered answered
         :correct correct
         :size 80}]]

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
  [skeleton-card
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


; --- Main Page Component ---
(defn question-sets-page []
  (r/create-class
   {:component-did-mount
    (fn []
      (let [sets-state @(state/get-sets-list-state)
            pagination-details (assoc (:pagination sets-state) :page 1) ; Always fetch page 1 on mount
            filters (:filters sets-state)]
        (http/get-tags (fn [_] nil))
        (http/get-sets filters pagination-details (fn [_] nil))))

    :reagent-render
    (fn []
      (let [sets-state @(state/get-sets-list-state)
            tags-state @(state/get-tags-state)
            sets-loading? (:loading? sets-state)
            tags-loading? (:loading? tags-state)
            error (:error sets-state)
            question-sets (:list sets-state)
            sets-pagination (:pagination sets-state)
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
                            (http/get-sets {:tags #{} :search ""} (assoc sets-pagination :page 1) (fn [_] nil)))}
              "Reset All"]]]

           (cond
             tags-loading?
             [:div {:class "mt-4 p-4"}
              [skeleton-text {:rows 2 :variant-width true}]]

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
                                                (assoc sets-pagination :page 1)
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
             (for [_ (range (get-in sets-pagination [:limit] 3))] ; Show a few skeleton cards based on limit or a default
               ^{:key (gensym "skeleton-set-")}
               [question-set-card-skeleton])]])
         ;; Content when not loading
         (when (and (empty? question-sets) (not error) (not sets-loading?)) ; Show empty state only if no error and not loading
           [:div {:class "mt-6"}
            [empty-state
             {:on-reset #(do
                           (state/set-sets-filter-tags #{})
                           (state/set-sets-filter-search "")
                           (http/get-sets {:tags #{} :search ""} (assoc sets-pagination :page 1) (fn [_] nil)))}]])
         ;; Question sets grid
         (when (seq question-sets)
           [:div
            [:div {:class "grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6 mt-6"}
             (for [set question-sets]
               ^{:key (:set-id set)}
               [question-set-card set])]

            ;; Always show pagination when there are question sets
            [pagination
             {:item-name "set"
              :page (:page sets-pagination)
              :total-pages (max 1 (:total_pages sets-pagination))
              :total-items (:total_items sets-pagination)
              :limit (:limit sets-pagination)
              :on-page-change #(do
                                 (state/set-sets-page %)
                                 (http/get-sets filters (assoc sets-pagination :page %) (fn [_] nil)))}]])]))}))