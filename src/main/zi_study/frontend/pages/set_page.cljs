(ns zi-study.frontend.pages.set-page
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.routes :as routes]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.badge :refer [badge]]
            [zi-study.frontend.components.alert :refer [alert]]
            [zi-study.frontend.components.input :refer [text-input]]
            [zi-study.frontend.components.toggle :refer [toggle]]
            [zi-study.frontend.components.dropdown :refer [dropdown menu-item]]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.skeleton :as skeleton]
            [zi-study.frontend.utilities :refer [cx]]

            [zi-study.frontend.components.questions.written-question :refer [written-question]]
            [zi-study.frontend.components.questions.mcq-single-question :refer [mcq-single-question]]
            [zi-study.frontend.components.questions.true-false-question :refer [true-false-question]]
            [zi-study.frontend.components.questions.mcq-multi-question :refer [mcq-multi-question]]
            [zi-study.frontend.components.questions.emq-question :refer [emq-question]]
            [zi-study.frontend.components.questions.cloze-question :refer [cloze-question]]
            [clojure.string :as str]
            ["lucide-react" :as lucide-icons]))

;; --- Skeleton Components ---

(defn- set-progress-header-skeleton []
  [:div {:class "mb-8 rounded-xl overflow-hidden shadow-sm border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-white dark:bg-[var(--color-dark-bg-paper)]"}
   [:div {:class "p-6"}
    [skeleton/skeleton {:variant :text :width "60%" :height "2rem" :class "mb-3"}] ;; Title
    [skeleton/skeleton {:variant :text :width "80%" :height "1rem" :class "mb-4"}]  ;; Description
    [:div {:class "flex flex-wrap gap-2 mb-6"} ;; Tags
     (for [_ (range 3)] ^{:key (gensym "tag-skel-")} [skeleton/skeleton {:variant :rectangular :width "5rem" :height "1.5rem"}])]
    [:div {:class "flex items-center justify-between"}
     [:div {:class "flex gap-6"} ;; Stats
      (for [_ (range 3)]
        ^{:key (gensym "stat-skel-")}
        [:div {:class "text-center"}
         [skeleton/skeleton {:variant :text :width "3rem" :height "2rem" :class "mb-1 mx-auto"}]
         [skeleton/skeleton {:variant :text :width "4rem" :height "0.8rem" :class "mx-auto"}]])]
     [:div {:class "w-32 h-32 relative flex items-center justify-center"} ;; Progress circle
      [skeleton/skeleton {:variant :circular :width "8rem" :height "8rem"}]]]]])

(defn- filter-controls-skeleton []
  [card {:class "mb-6"}
   [:div {:class "p-4"}
    [:div {:class "flex gap-4 mb-4"}
     [:div {:class "flex-grow"}
      [skeleton/skeleton {:variant :rectangular :height "2.5rem"}]] ;; Search input
     [:div {:class "flex-shrink-0"}
      [skeleton/skeleton {:variant :rectangular :width "8rem" :height "2.5rem"}]]] ;; Difficulty dropdown
    [:div {:class "flex items-center justify-between"}
     [:div {:class "flex items-center gap-4"}
      [skeleton/skeleton {:variant :rectangular :width "6rem" :height "2.5rem"}] ;; Status dropdown
      [skeleton/skeleton {:variant :rectangular :width "8rem" :height "2.5rem"}]] ;; Bookmark toggle
     [skeleton/skeleton {:variant :rectangular :width "7rem" :height "2.25rem"}]]] ;; Reset button
   ])

(defn- question-badge-strip-skeleton []
  [card {:class "mb-6" :variant :outlined}
   [:div {:class "p-4"}
    [:div {:class "flex items-center justify-between mb-3"}
     [skeleton/skeleton {:variant :text :width "40%" :height "1.25rem"}] ;; Title
     [skeleton/skeleton {:variant :rectangular :width "7rem" :height "2rem"}]] ;; Clear all button placeholder
    [:div {:class "px-4 pb-4 mt-3"}
     [:div {:class "flex flex-wrap gap-2"}
      (for [_ (range 10)] ^{:key (gensym "badge-skel-")} [skeleton/skeleton {:variant :circular :width "2rem" :height "2rem"}])]]]])

(defn- question-card-skeleton []
  [card {:class "mb-8" :variant :outlined}
   [:div {:class "p-5 flex items-start justify-between gap-4"}
    [:div {:class "flex items-start flex-grow"}
     [skeleton/skeleton {:variant :circular :width "2rem" :height "2rem" :class "mr-3"}] ;; Number badge
     [:div {:class "flex-grow"}
      [skeleton/skeleton {:variant :text :width "70%" :height "1.25rem" :class "mb-2"}] ;; Question text
      [skeleton/skeleton {:variant :text :width "50%" :height "1rem"}]]] ;; Retention hint placeholder
    [skeleton/skeleton {:variant :circular :width "2rem" :height "2rem"}]] ;; Bookmark button area
   [:div {:class "p-5 pt-0"}
    [skeleton/skeleton-text {:rows 3 :variant-width true :class "mb-4"}] ;; Placeholder for question content (options/textarea)
    [skeleton/skeleton {:variant :rectangular :width "8rem" :height "2.25rem" :class "ml-auto"}]] ;; Submit button area
   ])

(defn apply-filters [set-id]
  (let [current-filters @(state/get-current-set-filters)
        last-applied-filters (state/get-last-applied-filters)]
    (when (not= current-filters @last-applied-filters)
      (reset! last-applied-filters current-filters)
      (http/get-set-questions
       set-id
       current-filters
       (fn [{:keys [success data error]}]
         (if success
           (state/set-current-set-questions (:questions data))
           (do
             (println "Error updating questions with filters:" error)
             (state/set-current-set-questions-error (str "Failed to filter questions: " error)))))))))

(defn question-badge [{:keys [number answered? correct? on-click]}]
  [:div {:class "cursor-pointer transform hover:scale-110"
         :on-click on-click}
   [badge {:variant :outlined
           :color (cond
                    (not answered?) :secondary
                    correct? :success
                    :else :error)
           :size :md
           :rounded true
           :class "font-medium"}
    number]])

(defn question-badge-strip [{:keys [questions set-id]}]
  (let [answered-count (count (filter #(boolean (:user-answer %)) questions))
        clearing (r/atom false)]
    [card {:class "mb-6" :variant :outlined}
     [:div {:class "p-4"}
      [:div {:class "flex items-center justify-between mb-3"}
       [:div {:class "flex items-center gap-3"}
        [:> lucide-icons/Book {:size 20 :className "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"}]
        [:h3 {:class "text-sm font-medium"}
         "Questions Overview"]]
       (when (pos? answered-count)
         [button {:variant :outlined
                  :size :sm
                  :color :error
                  :class "text-xs"
                  :loading @clearing
                  :disabled @clearing
                  :start-icon lucide-icons/Trash2
                  :on-click (fn []
                              (when (js/confirm "Are you sure you want to clear all answers? This cannot be undone.")
                                (reset! clearing true)
                                (http/delete-answers set-id (fn [result]
                                                              (reset! clearing false)
                                                              (when-not (:success result)
                                                                (js/console.error "Failed to clear answers:" (:error result)))))))}
          (str "Clear All (" answered-count ")")])]]
     [:div {:class "px-4 pb-4"}
      [:div {:class "flex flex-wrap gap-2"}
       (map-indexed
        (fn [idx question]
          (let [question-id (:question-id question)
                answered? (boolean (:user-answer question))
                correct? (= 1 (get-in question [:user-answer :is-correct]))]
            ^{:key question-id}
            [question-badge
             {:number (inc idx)
              :answered? answered?
              :correct? correct?
              :on-click #(when-let [el (.getElementById js/document (str "question-" question-id))]
                           (.scrollIntoView el #js {:behavior "smooth"}))}]))
        questions)]]]))

(defn filter-controls []
  (fn [match]
    (let [set-id (get-in match [:parameters :path :set-id])
          rx-filters (state/get-current-set-filters)
          rx-search (r/reaction (:search @rx-filters))
          rx-bookmarked (r/reaction (:bookmarked @rx-filters))
          rx-difficulty (r/reaction (:difficulty @rx-filters))
          rx-answered (r/reaction (:answered @rx-filters))]
      [card {:class "mb-6"}
       [:div {:class "p-4"}
        ;; Search 
        [:div {:class "mb-4"}
         [text-input {:placeholder "Search questions..."
                      :value @rx-search
                      :start-icon lucide-icons/Search
                      :class "w-full"
                      :on-change #(state/set-current-set-filters (assoc @rx-filters :search (.. % -target -value)))
                      :on-change-debounced {:time 300 :callback #(apply-filters set-id)}}]]

        ;; Filter controls - Responsive layout
        [:div {:class "flex flex-wrap gap-2 items-center justify-between"}
         ;; Left side filters - Group on smaller screens
         [:div {:class "flex flex-wrap gap-2 items-center"}
          ;; Difficulty dropdown
          (r/with-let [difficulty-open? (r/atom false)]
            [:div {:class "flex-shrink-0 mb-2 sm:mb-0"}
             [dropdown {:trigger [button {:variant (if @rx-difficulty :primary :outlined)
                                          :size :sm
                                          :start-icon lucide-icons/BarChart2
                                          :class "whitespace-nowrap w-[120px]"}
                                  [:div {:class "flex items-center justify-between w-full"}
                                   [:span {:class "truncate max-w-[75px]"}
                                    (or (case @rx-difficulty
                                          1 "Easy"
                                          2 "Level 2"
                                          3 "Medium"
                                          4 "Level 4"
                                          5 "Hard"
                                          nil)
                                        "Difficulty")]
                                   [:span {:class "flex-shrink-0 ml-1 inline-flex"}
                                    [:> lucide-icons/ChevronDown {:size 14}]]]]
                        :open? difficulty-open?
                        :width :match-trigger
                        :min-width "120px"
                        :on-close #(apply-filters set-id)}

              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :difficulty nil))
                                       (reset! difficulty-open? false))
                          :selected? (nil? @rx-difficulty)}
               "Any Difficulty"]
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :difficulty 1))
                                       (reset! difficulty-open? false))
                          :selected? (= @rx-difficulty 1)}
               "Level 1 (Easy)"]
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :difficulty 2))
                                       (reset! difficulty-open? false))
                          :selected? (= @rx-difficulty 2)}
               "Level 2"]
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :difficulty 3))
                                       (reset! difficulty-open? false))
                          :selected? (= @rx-difficulty 3)}
               "Level 3 (Medium)"]
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :difficulty 4))
                                       (reset! difficulty-open? false))
                          :selected? (= @rx-difficulty 4)}
               "Level 4"]
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :difficulty 5))
                                       (reset! difficulty-open? false))
                          :selected? (= @rx-difficulty 5)}
               "Level 5 (Hard)"]]])

          ;; Status dropdown
          (r/with-let [answered-open? (r/atom false)]
            [:div {:class "flex-shrink-0 mb-2 sm:mb-0"}
             [dropdown {:trigger [button {:variant (if (not (nil? @rx-answered)) :primary :outlined)
                                          :size :sm
                                          :start-icon lucide-icons/CheckCircle
                                          :class "whitespace-nowrap w-[120px]"}
                                  [:div {:class "flex items-center justify-between w-full"}
                                   [:span {:class "truncate max-w-[75px]"}
                                    (case @rx-answered
                                      true "Answered"
                                      false "Unanswered"
                                      "Any Status")]
                                   [:span {:class "flex-shrink-0 ml-1 inline-flex"}
                                    [:> lucide-icons/ChevronDown {:size 14}]]]]
                        :open? answered-open?
                        :width :match-trigger
                        :min-width "120px"
                        :on-close #(apply-filters set-id)}
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :answered nil))
                                       (reset! answered-open? false))
                          :selected? (nil? @rx-answered)}
               "Any Status"]
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :answered true))
                                       (reset! answered-open? false))
                          :selected? (true? @rx-answered)}
               "Answered"]
              [menu-item {:on-click #(do
                                       (state/set-current-set-filters (assoc @rx-filters :answered false))
                                       (reset! answered-open? false))
                          :selected? (false? @rx-answered)}
               "Unanswered"]]])]

         ;; Right side - Bookmarked toggle + Reset
         [:div {:class "flex flex-wrap items-center gap-2"}
          ;; Bookmarked toggle
          [:div {:class "flex-shrink-0 mb-2 sm:mb-0"}
           [toggle {:checked (boolean @rx-bookmarked)
                    :label [:div {:class "flex items-center gap-1"}
                            [:> lucide-icons/Bookmark {:size 16}]
                            "Bookmarked"]
                    :container-style :pill
                    :size :sm
                    :on-change #(do
                                  (state/set-current-set-filters (assoc @rx-filters :bookmarked (not @rx-bookmarked)))
                                  (apply-filters set-id))}]]]]]])))

(defn question-component [question index]
  [:div {:id (str "question-" (:question-id question))
         :class "scroll-mt-4 transition-all duration-300 "}
   (case (:question-type question)
     "written" [written-question (assoc question :index index)]
     "mcq-single" [mcq-single-question (assoc question :index index)]
     "true-false" [true-false-question (assoc question :index index)]
     "mcq-multi" [mcq-multi-question (assoc question :index index)]
     "emq" [emq-question (assoc question :index index)]
     "cloze" [cloze-question (assoc question :index index)]
     [:div {:class "mb-6 p-4 border border-dashed rounded-md border-[var(--color-warning)] bg-[var(--color-warning-50)] dark:bg-[rgba(var(--color-warning-rgb),0.1)]"}
      [:div {:class "flex items-center"}
       [:> lucide-icons/AlertCircle {:size 20 :className "text-[var(--color-warning)] mr-2"}]
       (str "Unsupported question type: " (:question-type question))]])])

(defn no-questions-view [filters-val on-reset]
  [:div {:class "text-center py-12 border border-dashed rounded-xl border-[var(--color-light-border)] dark:border-[var(--color-dark-border)] bg-white dark:bg-[var(--color-dark-bg-paper)]"}
   [:> lucide-icons/Search
    {:size 48 :className "mx-auto mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]
   [:h3 {:class "text-xl font-medium mb-2"} "No questions found"]
   [:p {:class "mb-6 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
    "Try adjusting your filters or search terms."]
   (when (or (some? (:difficulty filters-val))
             (some? (:answered filters-val))
             (some? (:bookmarked filters-val))
             (not (str/blank? (:search filters-val))))
     [button {:variant :outlined
              :start-icon lucide-icons/RefreshCw
              :on-click on-reset}
      "Clear filters"])])

(defn questions-component []
  (fn [match]
    (let [set-id (get-in match [:parameters :path :set-id])
          current-set-full-state @(state/get-current-set-state)
          questions @(state/get-current-set-questions)
          questions-loading? (:questions-loading? current-set-full-state)
          questions-error (:questions-error current-set-full-state)]
      [:div
       ;; Badge strip: Show skeleton if questions are loading and we have no questions to display yet.
       ;; Otherwise, if questions exist, show the actual badge strip.
       (if (and questions-loading? (empty? questions))
         [question-badge-strip-skeleton]
         (when (seq questions)
           [question-badge-strip {:set-id set-id :questions questions}]))

       ;; Main content based on loading, error, or questions state
       (cond
         questions-loading?
         [:div {:class "mt-4"}
          (for [idx (range 3)] ^{:key (str "q-skel-" idx)} [question-card-skeleton])]

         questions-error
         [alert {:variant :soft :color :error :class "mt-4" :dismissible true :on-dismiss #(state/set-current-set-questions-error nil)}
          (str "Error loading questions: " questions-error)]

         (seq questions)
         [:div
          (doall (for [[index question] (map-indexed vector questions)]
                   ^{:key (:question-id question)}
                   [question-component question index]))]

         :else ;; No questions, not loading, no error => filters resulted in empty or set has no questions initially
         [no-questions-view
          @(state/get-current-set-filters)
          #(do
             (state/set-current-set-filters {:difficulty nil :answered nil :correct nil :bookmarked nil :search ""})
             (apply-filters set-id))])])))


(defn set-progress-header [{:keys [title description tags]}]
  (fn []
    (let [questions @(state/get-current-set-questions) ; This will react to changes
          total-questions (count questions)
          answered-count (count (filter #(boolean (:user-answer %)) questions))
          correct-count (count (filter #(= 1 (get-in % [:user-answer :is-correct])) questions))
          progress-percent (if (pos? total-questions) (Math/round (* 100 (/ answered-count total-questions))) 0)]
      [:div {:class "mb-8 rounded-xl overflow-hidden shadow-sm border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-white dark:bg-[var(--color-dark-bg-paper)]"}
       [:div {:class "p-6"}
        [:h1 {:class "text-2xl font-bold mb-2"} title]
        [:p {:class "mb-4 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"} description]

        (when (seq tags)
          [:div {:class "flex flex-wrap gap-2 mb-6"}
           (map-indexed (fn [idx tag]
                          ^{:key idx}
                          [badge {:variant :soft :color :secondary} tag])
                        tags)])

        [:div {:class "flex items-center justify-between"}
         [:div {:class "flex gap-6"}
          [:div {:class "text-center"}
           [:div {:class "text-3xl font-bold text-[var(--color-primary)]"}
            (str correct-count "/" total-questions)]
           [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"} "Correct"]]

          [:div {:class "text-center"}
           [:div {:class "text-3xl font-bold text-[var(--color-secondary)]"}
            (str (- answered-count correct-count) "/" total-questions)]
           [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"} "Incorrect"]]

          [:div {:class "text-center"}
           [:div {:class "text-3xl font-bold text-[var(--color-info)]"}
            (str (- total-questions answered-count) "/" total-questions)]
           [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"} "Remaining"]]]

         [:div {:class "w-32 h-32 relative flex items-center justify-center"}
          [:svg {:viewBox "0 0 100 100" :class "w-full h-full transform -rotate-90"}
           [:circle {:cx 50 :cy 50 :r 45 :fill "transparent"
                     :stroke "var(--color-light-divider)" :stroke-width 8}]]
          [:svg {:viewBox "0 0 100 100" :class "w-full h-full absolute top-0 left-0 transform -rotate-90"}
           [:circle {:cx 50 :cy 50 :r 45 :fill "transparent"
                     :stroke "var(--color-primary)" :stroke-width 8
                     :stroke-dasharray 283
                     :stroke-dashoffset (- 283 (* 283 (/ answered-count (if (pos? total-questions) total-questions 1))))}]]
          [:div {:class "absolute"}
           [:div {:class "text-2xl font-bold"} (str progress-percent "%")]]]]]])))


(defn set-page [match]
  (r/create-class
   {:component-did-mount
    (fn [_this]
      (let [set-id (get-in match [:parameters :path :set-id])
            query-params (when-let [search-str (.-search js/window.location)]
                           (when-not (str/blank? search-str)
                             (js/URLSearchParams. search-str)))
            focus-question-id (when query-params (.get query-params "focus-question-id"))
            initial-filters @(state/get-current-set-filters)]
        ;; Clear previous set data and errors before fetching new set
        (state/set-current-set nil [])
        (state/set-current-set-error nil)
        (state/set-current-set-questions-error nil)
        (state/set-current-set-loading true) ;; For set details

        (http/get-set-details set-id
                              (fn [{success-set-details :success set-details :data error-details :error}]
                                (if success-set-details
                                  (do
                                    (state/set-current-set-questions-loading true) ;; For questions of the new set
                                    (http/get-set-questions set-id initial-filters
                                                            (fn [{success-questions :success _questions-data :data error-questions :error}]
                                                              (if success-questions
                                                                (do
                                                                  ;; We now just set the details directly since questions go through the registry
                                                                  (swap! state/app-state assoc-in [:question-bank :current-set :details] set-details)
                                                                  ;; Explicitly turn off loading states
                                                                  (state/set-current-set-loading false)
                                                                  (state/set-current-set-questions-loading false)
                                                                  (when focus-question-id
                                                                    (js/setTimeout
                                                                     #(when-let [el (.getElementById js/document (str "question-" focus-question-id))]
                                                                        (.scrollIntoView el #js {:behavior "smooth" :block "center"}))
                                                                     500)))
                                                                (do
                                                                  (println "Error fetching questions:" error-questions)
                                                                  ;; The question IDs list will be empty due to using set-current-set-question-ids
                                                                  (swap! state/app-state assoc-in [:question-bank :current-set :details] set-details)
                                                                  (state/set-current-set-questions-error (str "Failed to load questions: " error-questions))
                                                                  ;; Explicitly turn off loading state
                                                                  (state/set-current-set-questions-loading false)
                                                                  (state/set-current-set-loading false))))))
                                  (do
                                    (println "Error fetching set details:" error-details)
                                    (state/set-current-set-error (str "Failed to load set details: " error-details))
                                    (state/set-current-set-loading false)
                                    (state/set-current-set-questions-loading false)))))))

    :component-will-unmount
    (fn []
      ;; Clear current set data but leave questions in the registry
      (swap! state/app-state assoc-in [:question-bank :current-set :details] nil)
      (swap! state/app-state assoc-in [:question-bank :current-set :questions] [])
      (state/set-current-set-error nil)
      (state/set-current-set-questions-error nil)
      (state/set-current-set-loading false)
      (state/set-current-set-questions-loading false))

    :reagent-render
    (fn [match]
      (let [current-set-state @(state/get-current-set-state)
            details (:details current-set-state)
            set-loading? (:loading? current-set-state)
            set-error (:error current-set-state)

            ;; Get from-folder from query parameters for render
            from-folder-id (when-let [search-str (.-search js/window.location)]
                             (when-not (str/blank? search-str)
                               (let [params (js/URLSearchParams. search-str)]
                                 (.get params "from-folder"))))
            from-search? (when-let [search-str (.-search js/window.location)]
                           (when-not (str/blank? search-str)
                             (let [params (js/URLSearchParams. search-str)]
                               (= "true" (.get params "from-search")))))]

        [:div {:class "max-w-3xl mx-auto px-4 py-8"}
         [:div {:class "mb-6"}
          [button {:variant :text
                   :start-icon lucide-icons/ArrowLeft
                   :class (cx "-ml-3 px-2 py-1 rounded-md transition-colors duration-150"
                              "text-[var(--color-light-back-button-text)] hover:text-[var(--color-light-back-button-text-hover)] hover:bg-[var(--color-light-back-button-bg-hover)]"
                              "dark:text-[var(--color-dark-back-button-text)] dark:hover:text-[var(--color-dark-back-button-text-hover)] dark:hover:bg-[var(--color-dark-back-button-bg-hover)]")
                   :on-click (cond
                               from-folder-id #(rfe/push-state routes/sym-folder-details-route {:folder-id (js/parseInt from-folder-id)})
                               from-search? #(rfe/push-state routes/sym-advanced-search-route)
                               :else #(rfe/push-state routes/sym-active-learning-question-sets-route))}
           (cond
             from-folder-id "Back to Folder"
             from-search? "Back to Search"
             :else "Back to Question Sets")]]

         (cond
           set-loading? ;; Initial loading for the set details
           [:div
            [set-progress-header-skeleton]
            [filter-controls-skeleton]
            [question-badge-strip-skeleton]
            [:div {:class "mt-4"}
             (for [idx (range 3)] ^{:key (str "page-skel-" idx)} [question-card-skeleton])]]

           set-error ;; Error loading set details
           [alert {:variant :soft :color :error :dismissible true
                   :on-dismiss #(state/set-current-set-error nil)
                   :class "mb-4"}
            set-error]

           details ;; Set details loaded successfully, now render content (questions-component handles its own loading/error for question list)
           [:div
            [set-progress-header {:title (:title details)
                                  :description (:description details)
                                  :tags (:tags details)}]
            [filter-controls match]
            [questions-component match]]

           :else ;; Fallback: Not loading, no error, but no details (should be rare if logic is correct)
           [:div {:class "text-center py-12 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
            "Initializing set data... or no set found."])]))}))