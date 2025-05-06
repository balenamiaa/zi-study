(ns zi-study.frontend.pages.bookmarks
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card card-header card-content]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.badge :refer [badge]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.components.input :refer [text-input]]
            ["lucide-react" :as lucide-icons]))

(defn bookmark-question-card [{:keys [question-id question-type question-data bookmarked-at]}]
  (let [question-title (cond
                         (= question-type "written") (:text question-data)
                         (= question-type "mcq-single") (:text question-data)
                         (= question-type "mcq-multi") (:text question-data)
                         (= question-type "true-false") (:text question-data)
                         (= question-type "cloze") (-> (:cloze_text question-data)
                                                       (clojure.string/replace #"\{\{c\d+::.*?\}\}" "____")
                                                       (clojure.string/replace #"\{\{c\d+\}\}" "____"))
                         (= question-type "emq") (:instructions question-data)
                         :else "Unknown question type")
        question-preview (if (> (count question-title) 150)
                           (str (subs question-title 0 150) "...")
                           question-title)
        date-bookmarked (when bookmarked-at
                          (-> bookmarked-at js/Date. .toLocaleDateString))
        icon (case question-type
               "written" lucide-icons/FileText
               "mcq-single" lucide-icons/CircleDot
               "mcq-multi" lucide-icons/CheckSquare
               "true-false" lucide-icons/ToggleLeft
               "cloze" lucide-icons/SquarePen
               "emq" lucide-icons/GitBranchPlus
               lucide-icons/HelpCircle)]

    [card {:hover-effect true
           :class "h-full animate-fade-in-up"
           :on-click #(rfe/push-state :zi-study.frontend.core/set-page {:set-id (:set-id question-data)})}
     [card-header {:title [:div {:class "flex items-center"}
                           [:> icon {:size 18 :className "mr-2 text-[var(--color-secondary)]"}]
                           [:span question-preview]]
                   :action [:div {:class "cursor-pointer text-[var(--color-primary)]"
                                  :on-click (fn [e]
                                              (.stopPropagation e)
                                              (http/toggle-bookmark question-id true (fn [_] nil)))}
                            [:> lucide-icons/Bookmark {:size 20}]]}]
     [card-content {}
      [:div {:class "flex flex-wrap text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] justify-between"}
       [:div
        [:> lucide-icons/Tag {:size 16 :className "inline mr-1"}]
        (str (clojure.string/capitalize question-type) " Question")]
       (when date-bookmarked
         [:div
          [:> lucide-icons/Calendar {:size 16 :className "inline mr-1"}]
          (str "Bookmarked " date-bookmarked)])]]]))

(defn bookmark-set-section [{:keys [set-id set-title questions]}]
  [:div {:class "mb-10"}
   [:div {:class "flex items-center justify-between mb-4"}
    [:h2 {:class "text-xl font-semibold"}
     [:span set-title]
     [:span {:class "ml-2 text-sm font-normal text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
      (str "(" (count questions) " bookmarked questions)")]]
    [button {:variant :outlined
             :size :sm
             :on-click #(rfe/push-state :zi-study.frontend.core/set-page {:set-id set-id})}
     "View Set"]]

   [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"}
    (for [question questions]
      ^{:key (:question-id question)}
      [bookmark-question-card question])]])

(defn bookmarks-page []
  (let [bookmarks-state (state/get-bookmarks-state)
        loading? (:loading? bookmarks-state)
        error (:error bookmarks-state)
        bookmarks (:list bookmarks-state)
        search-term (r/atom "")
        filtered-bookmarks (if (empty? @search-term)
                             bookmarks
                             (for [set-group bookmarks]
                               (update set-group :questions
                                       (fn [questions]
                                         (filter (fn [q]
                                                   (let [q-text (str (:question-data q))]
                                                     (clojure.string/includes?
                                                      (clojure.string/lower-case q-text)
                                                      (clojure.string/lower-case @search-term))))
                                                 questions)))))]

    ;; Load data on component mount
    (r/create-class
     {:component-did-mount
      (fn []
        (http/get-bookmarks (fn [_] nil)))

      :reagent-render
      (fn []
        [:div {:class "container mx-auto px-4 py-8"}
         [:div {:class "flex justify-between items-center mb-6"}
          [:h1 {:class "text-2xl font-bold"} "My Bookmarks"]
          [:div
           [button {:variant :outlined
                    :start-icon lucide-icons/Book
                    :on-click #(rfe/push-state :zi-study.frontend.core/question-sets)}
            "All Question Sets"]]]

         ;; Search
         [:div {:class "mb-8"}
          [text-input {:placeholder "Search bookmarked questions..."
                       :value @search-term
                       :start-icon lucide-icons/Search
                       :on-change #(reset! search-term (.. % -target -value))
                       :class "w-full"}]]

         ;; Error handling
         (when error
           [alert {:variant :soft
                   :color :error
                   :dismissible true
                   :on-dismiss #(state/set-bookmarks-error nil)
                   :class "mb-4"}
            error])

         ;; Loading state
         (when loading?
           [:div {:class "flex justify-center items-center py-12"}
            [:> lucide-icons/Loader {:size 32 :className "animate-spin text-[var(--color-primary)]"}]])

         ;; Empty state
         (when (and (not loading?) (empty? bookmarks))
           [:div {:class "text-center py-12 border border-dashed rounded-lg border-[var(--color-light-border)] dark:border-[var(--color-dark-border)]"}
            [:> lucide-icons/Bookmark
             {:size 48 :className "mx-auto mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]
            [:h3 {:class "text-xl font-medium mb-2"} "No bookmarks yet"]
            [:p {:class "mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
             "Bookmark questions to save them for later review"]
            [button {:variant :primary
                     :on-click #(rfe/push-state :zi-study.frontend.core/question-sets)}
             "Browse Question Sets"]])

         ;; Empty search results
         (when (and (not loading?)
                    (not (empty? bookmarks))
                    (empty? (remove #(empty? (:questions %)) filtered-bookmarks)))
           [:div {:class "text-center py-12 border border-dashed rounded-lg border-[var(--color-light-border)] dark:border-[var(--color-dark-border)]"}
            [:> lucide-icons/Search
             {:size 48 :className "mx-auto mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]
            [:h3 {:class "text-xl font-medium mb-2"} "No matching bookmarks"]
            [:p {:class "mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
             "Try a different search term"]
            [button {:variant :outlined
                     :start-icon lucide-icons/X
                     :on-click #(reset! search-term "")}
             "Clear Search"]])

         ;; Bookmarks by set
         (for [set-group (remove #(empty? (:questions %)) filtered-bookmarks)]
           ^{:key (:set-id set-group)}
           [bookmark-set-section set-group])])})))