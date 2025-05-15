(ns zi-study.frontend.pages.advanced-search
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.input :refer [text-input]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.skeleton :as skeleton]
            [zi-study.frontend.pages.question-sets :refer [pagination]] ;; Re-use pagination
            ;; Import question components
            [zi-study.frontend.components.questions.written-question :refer [written-question]]
            [zi-study.frontend.components.questions.mcq-single-question :refer [mcq-single-question]]
            [zi-study.frontend.components.questions.true-false-question :refer [true-false-question]]
            [zi-study.frontend.components.questions.mcq-multi-question :refer [mcq-multi-question]]
            [zi-study.frontend.components.questions.emq-question :refer [emq-question]]
            [zi-study.frontend.components.questions.cloze-question :refer [cloze-question]]
            [clojure.string :as str]
            ["lucide-react" :as lucide-icons]))

(defn- question-card-skeleton [] ; Simplified skeleton for search results
  [card {:class "mb-6"}
   [:div {:class "p-5"}
    [skeleton/skeleton {:variant :text :width "30%" :height "1.25rem" :class "mb-3"}] ; Set title placeholder
    [skeleton/skeleton {:variant :rectangular :width "100%" :height "6rem" :class "mb-3"}] ; Question content placeholder
    [skeleton/skeleton {:variant :rectangular :width "8rem" :height "2.25rem"}]]]) ; View in set button placeholder

(defn- searched-question-card [{:keys [question index]}]
  (let [{:keys [question-id set-id set-title question-type]} question]
    [card {:class "mb-6 animate-fade-in-up"}
     [:div {:class "p-5"}
      [:div {:class "mb-3 pb-3 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
       [:p {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mb-1"}
        "From set:"]
       [:h3 {:class "text-md font-semibold text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
        set-title]]
      ;; Render the actual question using existing components
      (case question-type
        "written" [written-question (assoc question :index index :standalone true)]
        "mcq-single" [mcq-single-question (assoc question :index index :standalone true)]
        "true-false" [true-false-question (assoc question :index index :standalone true)]
        "mcq-multi" [mcq-multi-question (assoc question :index index :standalone true)]
        "emq" [emq-question (assoc question :index index :standalone true)]
        "cloze" [cloze-question (assoc question :index index :standalone true)]
        [:div {:class "text-red-500"} (str "Unsupported question type: " question-type)])

      [:div {:class "mt-4 pt-4 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] flex justify-end"}
       [button {:variant :soft
                :color :primary
                :size :sm
                :start-icon lucide-icons/ExternalLink
                :on-click #(rfe/push-state :zi-study.frontend.core/set-page
                                           {:set-id set-id}
                                           {:focus_question_id question-id})}
        "View in Set"]]]]))

(defn- empty-search-results []
  [:div {:class "text-center py-12 border border-dashed rounded-xl border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"}
   [:> lucide-icons/SearchX {:size 48 :className "mx-auto mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]
   [:h3 {:class "text-xl font-medium mb-2 text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
    "No Questions Found"]
   [:p {:class "text-md text-center text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] max-w-md"}
    "Try refining your search terms or explore question sets directly."]])

(defn advanced-search-page [_match]
  (let [last-searched-keywords (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [_this]
        (let [filters @(state/get-advanced-search-filters-state)
              pagination @(state/get-advanced-search-results-pagination-state)]
          (when-not (str/blank? (:keywords filters))
            (http/advanced-search-questions filters pagination (fn [_] nil)))))

      :reagent-render
      (fn []
        (let [results-state @(state/get-advanced-search-results-state)
              loading? (:loading? results-state)
              error (:error results-state)
              questions-list (:list results-state)
              pagination-info (:pagination results-state)]

          [:div {:class "p-4 md:p-6 lg:p-8 animate-fade-in-up"}
           [:div {:class "flex items-center mb-6 pb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
            [:> lucide-icons/SearchCode {:size 32 :class "mr-3 text-[var(--color-primary)]"}]
            [:h1 {:class "text-3xl font-semibold tracking-tight text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
             "Advanced Question Search"]]

           ;; Search Input
           (let [filters-state @(state/get-advanced-search-filters-state)
                 keywords (:keywords filters-state)]
             [:div {:class "mb-8"}
              [text-input
               {:placeholder "Enter keywords to search across all questions..."
                :value keywords
                :start-icon lucide-icons/Search
                :on-change #(state/set-advanced-search-keywords (.. % -target -value))
                :on-change-debounced {:time 300
                                      :callback (fn [event]
                                                  (let [search-val (.. event -target -value)]
                                                    (reset! last-searched-keywords search-val)
                                                    (if (str/blank? search-val)
                                                      (state/set-advanced-search-results [] (:pagination @(state/get-advanced-search-results-state)))
                                                      (http/advanced-search-questions
                                                       (assoc @(state/get-advanced-search-filters-state) :keywords search-val)
                                                       (assoc pagination-info :page 1)
                                                       (fn [_] nil)))))}}]])


           (cond
           ; Loading Display
             loading?
             [:div
              (for [i (range 3)]
                ^{:key (str "search-skel-" i)} [question-card-skeleton])]

           ; Error Display
             error
             [alert {:variant :soft :color :error :dismissible true
                     :on-dismiss #(state/set-advanced-search-error nil)
                     :class "mb-6"}
              error]

           ; Results Display
             :else
             (if (seq questions-list)
               [:div
                [:p {:class "mb-4 text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
                 (str "Found " (:total_items pagination-info) " questions. "
                      "Displaying page " (:page pagination-info) " of " (:total_pages pagination-info) ".")]
                (doall (map-indexed
                        (fn [idx question]
                          ^{:key (:question-id question)}
                          [searched-question-card {:question question :index idx}])
                        questions-list))
                (when (> (:total_pages pagination-info) 1)
                  [pagination
                   {:page (:page pagination-info)
                    :total-pages (:total_pages pagination-info)
                    :total-items (:total_items pagination-info)
                    :limit (:limit pagination-info)
                    :on-page-change #(do
                                       (state/set-advanced-search-page %)
                                       (http/advanced-search-questions
                                        @(state/get-advanced-search-filters-state)
                                        (assoc pagination-info :page %)
                                        (fn [_] nil)))}])]
               (when (and (not (str/blank? @last-searched-keywords)) (empty? questions-list))
                 [empty-search-results])))]))})))