(ns zi-study.frontend.pages.set-page
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
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


;; Question Type Components


(defn- bookmark-toggle-loading? [question-id]
  (:loading? @(state/get-bookmark-toggle-state question-id)))

(defn bookmark-button [{:keys [question-id bookmarked]}]
  (let [loading? (bookmark-toggle-loading? question-id)]
    [:div {:class (str "cursor-pointer inline-flex items-center justify-center w-8 h-8 rounded-full transition-all "
                       (if (and bookmarked (not loading?)) ; Don't show colored bg if loading
                         "bg-[var(--color-primary-100)] dark:bg-[rgba(var(--color-primary-rgb),0.2)]"
                         "hover:bg-[var(--color-light-bg)] dark:hover:bg-[rgba(255,255,255,0.05)]"))
           :on-click #(when-not loading?
                        (http/toggle-bookmark question-id (not bookmarked) (fn [_] nil)))}
     (if loading?
       [:> lucide-icons/Loader2 {:size 20 :className "animate-spin text-[var(--color-primary)]"}]
       [:> (if bookmarked
             lucide-icons/Bookmark
             lucide-icons/BookmarkPlus)
        {:size 20
         :className (if bookmarked "text-[var(--color-primary)]" "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]")}])]))

(defn question-number-badge [number answered? correct?]
  [:div {:class (str "w-8 h-8 rounded-full flex items-center justify-center font-medium text-sm mr-3 "
                     (cond
                       (and answered? correct?) "bg-[var(--color-success-100)] text-[var(--color-success)] dark:bg-[rgba(var(--color-success-rgb),0.2)]"
                       answered? "bg-[var(--color-error-100)] text-[var(--color-error)] dark:bg-[rgba(var(--color-error-rgb),0.2)]"
                       :else "bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"))}
   number])

(defn retention-hint [{:keys [text]}]
  (when text
    [:div {:class "mt-3 px-4 py-3 rounded-md bg-[var(--color-secondary-50)] dark:bg-[rgba(var(--color-secondary-rgb),0.1)] flex items-start"}
     [:> lucide-icons/Lightbulb {:size 18 :className "text-[var(--color-secondary)] mt-0.5 mr-2 flex-shrink-0"}]
     [:span {:class "text-sm text-[var(--color-secondary-800)] dark:text-[var(--color-secondary-200)]"} text]]))


;; Helper to get and dereference the answer submission state reaction
(defn- get-deref-answer-submission-state [question-id]
  @(state/get-answer-submission-state question-id))

;; Helper to get and dereference the self-eval state reaction
(defn- get-deref-self-eval-state [question-id]
  @(state/get-self-eval-state question-id))

(defn clear-answer [question-id callback]
  (http/delete-answer question-id (fn [{:keys [success error]}]
                                    (if success
                                      (callback)
                                      (js/console.error "Failed to clear answer:" error)))))

(defn clear-answers [set-id callback]
  (http/delete-answers set-id (fn [{:keys [success error]}]
                                (if success
                                  (callback)
                                  (js/console.error "Failed to clear answers:" error)))))

(defn explanation-section [{:keys [explanation show-explanation? on-toggle]}]
  (when explanation
    [:div {:class "mt-5 pt-4 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
     [:div {:class "flex justify-center mb-2"}
      [button {:variant :outlined
               :size :sm
               :class "w-full max-w-xs"
               :start-icon (if show-explanation? lucide-icons/EyeOff lucide-icons/Eye)
               :on-click on-toggle}
       (if show-explanation? "Hide Explanation" "Show Explanation")]]
     (when show-explanation?
       [:div {:class "mt-3 p-4 bg-[var(--color-info-50)] dark:bg-[rgba(var(--color-info-rgb),0.1)] rounded-md"}
        [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
         explanation]])]))

(defn question-header
  "Common header component for questions"
  [{:keys [index question-id text retention-aid submitted? is-correct? bookmarked clear-fn]}]
  (let [clearing (r/atom false)]
    [:div {:class "p-5 flex items-start justify-between gap-4"}
     [:div {:class "flex items-start flex-grow"}
      [question-number-badge index submitted? is-correct?]
      [:div
       [:div {:class "font-medium text-lg mb-1"} text]
       (when (and submitted? retention-aid)
         [retention-hint {:text retention-aid}])]]
     [:div {:class "flex items-center gap-2"}
      (when submitted?
        [button {:variant :text
                 :size :sm
                 :color :error
                 :loading @clearing
                 :disabled @clearing
                 :start-icon lucide-icons/Trash2
                 :on-click (fn []
                             (reset! clearing true)
                             (clear-fn (fn [_] (reset! clearing false))))}
         "Clear"])
      [bookmark-button {:question-id question-id
                        :bookmarked bookmarked}]]]))

(defn written-question []
  (let [answer-text (r/atom "")
        show-explanation? (r/atom false)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [[props] (r/argv this)
              initial-answer (get-in (:user-answer props) [:answer-data :answer] "")]
          (reset! answer-text initial-answer)))

      :component-did-update
      (fn [this [_ old-props]]
        (let [[new-props] (r/argv this)
              old-answer (get-in (:user-answer old-props) [:answer-data :answer] ::not-found) ; Use sentinel for comparison
              new-answer (get-in (:user-answer new-props) [:answer-data :answer] ::not-found)]
          (when (not= old-answer new-answer)
            (reset! answer-text (if (= new-answer ::not-found) "" new-answer)))))

      :reagent-render
      (fn [props]
        (let [{:keys [question-id question-data user-answer retention-aid bookmarked index]} props
              submission-state (get-deref-answer-submission-state question-id)
              self-eval-state (get-deref-self-eval-state question-id)
              submitted? (boolean user-answer)
              self-evaluated? (and submitted? (some? (:is-correct user-answer)))
              is-correct? (= 1 (:is-correct user-answer))
              text (:text question-data)
              correct-answer (:correct_answer question-data)
              explanation (:explanation question-data)
              answer-loading? (:loading? submission-state)
              eval-loading? (:loading? self-eval-state)]

          [card {:class "mb-8" :variant :outlined}
           [question-header {:index (inc index)
                             :question-id question-id
                             :text text
                             :retention-aid retention-aid
                             :submitted? submitted?
                             :is-correct? is-correct?
                             :bookmarked bookmarked
                             :clear-fn (fn [callback]
                                         (http/delete-answer question-id callback))}]

           [:div {:class "p-5 pt-0"}
            (if (and submitted? (not self-evaluated?))
              [:div
               [:div {:class "mb-4 p-4 bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] rounded-md"}
                [:div {:class "font-medium mb-2"} "Your answer:"]
                [:p (get-in user-answer [:answer-data :answer])]]

               [:div {:class "mb-4 p-4 bg-[var(--color-primary-50)] dark:bg-[rgba(var(--color-primary-rgb),0.1)] rounded-md"}
                [:div {:class "font-medium mb-2 text-[var(--color-primary)]"} "Correct answer:"]
                [:p correct-answer]]

               (when explanation
                 [explanation-section {:explanation explanation
                                       :show-explanation? @show-explanation?
                                       :on-toggle #(swap! show-explanation? not)}])

               [:div {:class "flex justify-center gap-4 mt-6"}
                [button {:variant :outlined
                         :color :error
                         :start-icon lucide-icons/X
                         :disabled eval-loading?
                         :loading eval-loading?
                         :class "flex-1 max-w-40"
                         :on-click #(http/self-evaluate question-id false (fn [_] nil))}
                 "I was wrong"]
                [button {:variant :primary
                         :start-icon lucide-icons/Check
                         :disabled eval-loading?
                         :loading eval-loading?
                         :class "flex-1 max-w-40"
                         :on-click #(http/self-evaluate question-id true (fn [_] nil))}
                 "I was right"]]]

              [:div
               (when self-evaluated?
                 [:div {:class (str "mb-6 p-4 rounded-md "
                                    (if is-correct?
                                      "bg-[var(--color-success-50)] dark:bg-[rgba(var(--color-success-rgb),0.1)]"
                                      "bg-[var(--color-error-50)] dark:bg-[rgba(var(--color-error-rgb),0.1)]"))}
                  [:div {:class (str "font-medium mb-3 flex items-center "
                                     (if is-correct?
                                       "text-[var(--color-success)]"
                                       "text-[var(--color-error)]"))}
                   [:> (if is-correct? lucide-icons/CheckCircle lucide-icons/XCircle) {:size 20 :className "mr-2"}]
                   (if is-correct?
                     "You marked this as correct"
                     "You marked this as incorrect")]

                  [:div {:class "mb-4"}
                   [:div {:class "font-medium text-sm mb-1"} "Your answer:"]
                   [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] p-2 bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] rounded-md"}
                    (get-in user-answer [:answer-data :answer])]]

                  [:div
                   [:div {:class "font-medium text-sm mb-1"} "Correct answer:"]
                   [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] p-2 bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] rounded-md"}
                    correct-answer]]

                  (when explanation
                    [explanation-section {:explanation explanation
                                          :show-explanation? @show-explanation?
                                          :on-toggle #(swap! show-explanation? not)}])])

               (when (not submitted?)
                 [:div {:class "mt-4"}
                  [:textarea {:value @answer-text
                              :placeholder "Enter your answer here..."
                              :disabled answer-loading?
                              :on-change #(reset! answer-text (.. % -target -value))
                              :class (str "w-full p-3 border rounded-md bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] "
                                          "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] "
                                          "focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-300)] "
                                          "min-h-[120px] text-[var(--color-light-text)] dark:text-[var(--color-dark-text)]")}]
                  [:div {:class "mt-3 flex justify-end gap-2"}
                   [button {:variant :primary
                            :disabled (or answer-loading? (str/blank? @answer-text))
                            :loading answer-loading?
                            :on-click #(when (not (str/blank? @answer-text))
                                         (http/submit-answer question-id "written" {:answer @answer-text} (fn [_] nil)))}
                    "Submit Answer"]]])])]]))})))


(defn mcq-single-option
  "A single MCQ option component"
  [{:keys [index option is-selected is-correct is-actually-correct answered-globally pending-globally on-click]}]
  [:div {:class (str "mb-3 p-4 rounded-md border-2 transition-all flex items-center "
                     (if pending-globally "cursor-wait" "cursor-pointer ")
                     (cond
                       ;; Pending state for the selected item (this specific option is being submitted)
                       (and is-selected pending-globally) ;; If this option is selected and a submission is pending
                       "border-[var(--color-primary)] bg-[rgba(var(--color-primary-rgb),0.05)] animate-pulse"

                       ;; This option was submitted and is correct
                       (and answered-globally is-selected is-correct)
                       "border-[var(--color-success)] bg-[var(--color-success-50)] dark:bg-[rgba(var(--color-success-rgb),0.1)]"

                       ;; This option was submitted and is incorrect
                       (and answered-globally is-selected (not is-correct))
                       "border-[var(--color-error)] bg-[var(--color-error-50)] dark:bg-[rgba(var(--color-error-rgb),0.1)]"

                       ;; This option is selected by the user for a new/changed answer, but not yet submitted (or submission not pending globally yet)
                       (and is-selected (not answered-globally) (not pending-globally))
                       "border-[var(--color-primary-300)] dark:border-[var(--color-primary-400)]"

                       ;; Default state or if another option is selected
                       :else
                       "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] hover:border-[var(--color-primary-300)] dark:hover:border-[var(--color-primary-400)]"))
         :on-click (when (not pending-globally) on-click)}

   [:div {:class (str "w-6 h-6 rounded-full flex-shrink-0 flex items-center justify-center mr-3 "
                      (cond
                        (and is-selected pending-globally) "border-2 border-[var(--color-primary)]"
                        (and answered-globally is-selected is-correct) "border-2 border-[var(--color-success)]"
                        (and answered-globally is-selected (not is-correct)) "border-2 border-[var(--color-error)]"
                        is-selected "border-2 border-[var(--color-primary)]"
                        :else "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"))}
    (cond
      (and is-selected pending-globally) ; Spinner if this option is selected and a submission is globally pending
      [:> lucide-icons/Loader2 {:size 16 :className "text-[var(--color-primary)] animate-spin"}]

      (and answered-globally is-selected is-correct)
      [:> lucide-icons/Check {:size 16 :className "text-[var(--color-success)]"}]

      (and answered-globally is-selected (not is-correct))
      [:> lucide-icons/X {:size 16 :className "text-[var(--color-error)]"}]

      is-selected ; Selected but not yet answered-globally and not pending
      [:div {:class "w-3 h-3 rounded-full bg-[var(--color-primary)]"}]

      :else nil)]

   [:div {:class (str "flex-grow " (when (and is-selected pending-globally) "text-[var(--color-primary)]"))} option]

   ;; Show check mark on the *actually correct* option if the question has been answered globally and not pending
   (when (and answered-globally (not pending-globally) is-actually-correct)
     [:> lucide-icons/Check {:size 20 :className "ml-auto text-[var(--color-success)]"}])
   ;; Show X mark on a submitted incorrect option if question answered globally and not pending
   (when (and answered-globally (not pending-globally) is-selected (not is-correct))
     [:> lucide-icons/X {:size 20 :className "ml-auto text-[var(--color-error)]"}])])

(defn mcq-single-question []
  (let [current-selected-idx-atom (r/atom nil) ; User's current visual selection, before/during submission
        show-explanation? (r/atom false)
        submitting-via-local-flag? (r/atom false)] ; To track submission initiated by this component
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [[props] (r/argv this)
              initial-submitted-answer (get-in (:user-answer props) [:answer-data :answer])]
          (reset! current-selected-idx-atom initial-submitted-answer) ; Initialize with submitted answer if exists
          ))

      :component-did-update
      (fn [this [_ old-props]]
        (let [[new-props] (r/argv this)
              old-submitted-answer (get-in (:user-answer old-props) [:answer-data :answer])
              new-submitted-answer (get-in (:user-answer new-props) [:answer-data :answer])]
          (when (not= old-submitted-answer new-submitted-answer)
            ;; If user_answer changed (e.g. after submission), update current visual selection to match, 
            ;; unless a new submission is already in progress locally.
            (when-not @submitting-via-local-flag?
              (reset! current-selected-idx-atom new-submitted-answer)))))

      :reagent-render
      (fn [props]
        (let [{:keys [question-id question-data user-answer retention-aid bookmarked index]} props

              ;; State from global app-state (reagent reactions)
              submission-state-from-global (get-deref-answer-submission-state question-id)

              ;; Has any answer been successfully submitted and recorded in user-answer?
              is-globally-answered? (boolean user-answer)
              ;; What was the index of the globally confirmed answer?
              globally-submitted-idx (get-in user-answer [:answer-data :answer])

              ;; Is a submission currently in progress (either globally or initiated locally)?
              is-submission-pending-globally? (or (:loading? submission-state-from-global) @submitting-via-local-flag?)

              text (:text question-data)
              options (:options question-data)
              actual-correct-idx (:correct_index question-data)
              explanation (:explanation question-data)

              handle-option-click (fn [clicked-idx]
                                    (when (not is-submission-pending-globally?) ; Prevent action if already submitting
                                      (reset! current-selected-idx-atom clicked-idx) ; Optimistically set visual selection
                                      (reset! submitting-via-local-flag? true)      ; Set local submission flag
                                      (http/submit-answer question-id "mcq-single" {:answer clicked-idx}
                                                          (fn [_result]
                                                            (reset! submitting-via-local-flag? false) ; Reset local flag on completion
                                                            ;; user-answer in props will update via state change, triggering re-render
                                                            ))))

              clear-fn (fn [callback]
                         (http/delete-answer question-id callback)
                         (reset! current-selected-idx-atom nil))]

          [card {:class "mb-8" :variant :outlined}
           [question-header {:index (inc index)
                             :question-id question-id
                             :text text
                             :retention-aid retention-aid
                             :submitted? is-globally-answered?
                             :is-correct? (when is-globally-answered? (= actual-correct-idx globally-submitted-idx))
                             :bookmarked bookmarked
                             :clear-fn clear-fn}]

           [:div {:class "px-5 pb-5"}
            (doall
             (for [[idx option-text] (map-indexed vector options)]
               (let [;; Determine if this option is the one currently visually selected by the user OR the one pending submission
                     is-this-option-visually-selected?
                     (if is-submission-pending-globally?
                       (= idx @current-selected-idx-atom) ; If pending, visual selection is based on the atom
                       (if is-globally-answered?
                         (= idx globally-submitted-idx) ; If answered & not pending, visual is the submitted one
                         (= idx @current-selected-idx-atom))) ; If not answered & not pending, visual is atom

                     ;; Is this specific option the one that was submitted and confirmed as correct/incorrect?
                     is-this-option-submitted-and-correct? (and is-globally-answered?
                                                                (= idx globally-submitted-idx)
                                                                (= idx actual-correct-idx))
                     is-this-option-submitted-and-incorrect? (and is-globally-answered?
                                                                  (= idx globally-submitted-idx)
                                                                  (not= idx actual-correct-idx))]
                 ^{:key idx}
                 [mcq-single-option {:index idx
                                     :option option-text
                                     :is-selected is-this-option-visually-selected?
                                     ;; For styling the option itself (e.g., green/red border after submission)
                                     :is-correct (if is-this-option-submitted-and-correct? true (if is-this-option-submitted-and-incorrect? false nil))
                                     ;; For the checkmark on the far right, indicating the *actual* correct answer
                                     :is-actually-correct (= idx actual-correct-idx)
                                     :answered-globally is-globally-answered?
                                     :pending-globally is-submission-pending-globally?
                                     :on-click #(handle-option-click idx)}])))]

           ;; Show explanation only after a submission has been processed (not pending) and if globally answered
           (when (and is-globally-answered? (not is-submission-pending-globally?) explanation)
             [explanation-section {:explanation explanation
                                   :show-explanation? @show-explanation?
                                   :on-toggle #(swap! show-explanation? not)}])]))})))

(defn apply-filters [set-id]
  (state/set-current-set-questions-loading true)
  (http/get-set-questions
   set-id
   @(state/get-current-set-filters)
   (fn [{:keys [success data error]}]
     (if success
       (state/set-current-set-questions (:questions data))
       (do
         (println "Error updating questions with filters:" error)
         (state/set-current-set-questions-error (str "Failed to filter questions: " error))))
     ;; set-current-set-questions and set-current-set-questions-error handle questions-loading? false
     )))


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
        ;; Search and difficulty row
        [:div {:class "flex gap-4 mb-4"}
         [:div {:class "flex-grow"}
          [text-input {:placeholder "Search questions..."
                       :value @rx-search
                       :start-icon lucide-icons/Search
                       :class "w-full"
                       :on-change #(state/set-current-set-filters (assoc @rx-filters :search (.. % -target -value)))
                       :on-change-debounced {:time 300 :callback #(apply-filters set-id)}}]]
         [:div {:class "flex-shrink-0"}
          [dropdown {:trigger [button {:variant :outlined
                                       :start-icon lucide-icons/BarChart2
                                       :class (when @rx-difficulty "text-primary dark:text-primary-300")}
                               (or (case @rx-difficulty
                                     1 "Easy"
                                     2 "Level 2"
                                     3 "Medium"
                                     4 "Level 4"
                                     5 "Hard"
                                     nil)
                                   "Difficulty")]
                     :options [{:value nil :label "Any difficulty"}
                               {:value 1 :label "Level 1 (Easy)"}
                               {:value 2 :label "Level 2"}
                               {:value 3 :label "Level 3 (Medium)"}
                               {:value 4 :label "Level 4"}
                               {:value 5 :label "Level 5 (Hard)"}]
                     :on-close #(apply-filters set-id)}
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :difficulty nil))} "Any Difficulty"]
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :difficulty 1))} "Level 1 (Easy)"]
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :difficulty 2))} "Level 2"]
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :difficulty 3))} "Level 3 (Medium)"]
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :difficulty 4))} "Level 4"]
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :difficulty 5))} "Level 5 (Hard)"]]]]

        ;; Status and bookmarks row
        [:div {:class "flex items-center justify-between"}
         [:div {:class "flex items-center gap-4"}
          [dropdown {:trigger [button {:variant :outlined
                                       :start-icon lucide-icons/CheckCircle
                                       :class (when @rx-answered "text-primary dark:text-primary-300")}
                               (case @rx-answered
                                 true "Answered"
                                 false "Unanswered"
                                 "Status")]
                     :on-close #(apply-filters set-id)}
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :answered nil))}
            "Any status"]
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :answered true))}
            "Answered"]
           [menu-item {:on-click #(state/set-current-set-filters (assoc @rx-filters :answered false))}
            "Unanswered"]]

          [toggle {:checked (boolean @rx-bookmarked)
                   :label [:div {:class "flex items-center gap-1"}
                           [:> lucide-icons/Bookmark {:size 16}]
                           "Bookmarked only"]
                   :on-change #(do
                                 (state/set-current-set-filters (assoc @rx-filters :bookmarked (not @rx-bookmarked)))
                                 (apply-filters set-id))}]]

         ;; Reset button
         [button {:variant :text
                  :size :sm
                  :start-icon lucide-icons/RefreshCw
                  :class (when (or (some? @rx-difficulty)
                                   (some? @rx-answered)
                                   (some? @rx-bookmarked)
                                   (not (str/blank? @rx-search)))
                           "text-primary dark:text-primary-300")
                  :on-click #(do
                               (state/set-current-set-filters {:difficulty nil
                                                               :answered nil
                                                               :correct nil
                                                               :bookmarked nil
                                                               :search ""})
                               (apply-filters set-id))}
          "Reset filters"]]]])))

(defn question-component [question index]
  [:div {:id (str "question-" (:question-id question))
         :class "scroll-mt-4 transition-all duration-300 "}
   (case (:question-type question)
     "written" [written-question (assoc question :index index)]
     "mcq-single" [mcq-single-question (assoc question :index index)]
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
  (fn [match] ; match is passed from parent (set-page)
    (let [set-id (get-in match [:parameters :path :set-id])
          current-set-full-state @(state/get-current-set-state)
          questions (:questions current-set-full-state)
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
  (fn [] ; Removed unnecessary inner fn
    (let [questions @(state/get-current-set-questions) ; This will react to changes
          total-questions (count questions)
          answered-count (count (filter #(boolean (:user-answer %)) questions))
          correct-count (count (filter #(= 1 (get-in % [:user-answer :is-correct])) questions))
          progress-percent (if (pos? total-questions) (Math/round (* 100 (/ correct-count total-questions))) 0)]
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
                     :stroke-dashoffset (- 283 (* 283 (/ correct-count (if (pos? total-questions) total-questions 1))))}]]
          [:div {:class "absolute"}
           [:div {:class "text-2xl font-bold"} (str progress-percent "%")]]]]]])))


(defn set-page [match]
  (r/create-class
   {:component-did-mount
    (fn [_this]
      (let [set-id (get-in match [:parameters :path :set-id])
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
                                                            (fn [{success-questions :success questions-data :data error-questions :error}]
                                                              (if success-questions
                                                                (state/set-current-set set-details (:questions questions-data))
                                                                (do
                                                                  (println "Error fetching questions:" error-questions)
                                                                  (state/set-current-set set-details []) ;; Set details, empty questions
                                                                  (state/set-current-set-questions-error (str "Failed to load questions: " error-questions))
                                                                  ;; state/set-current-set will turn off loading? and questions-loading?
                                                                  ;; but explicitly turn off questions-loading if set-current-set wasn't fully successful for questions
                                                                  (state/set-current-set-questions-loading false)
                                                                  (state/set-current-set-loading false) ; Ensure overall is also off if we only partially loaded
                                                                  )))))
                                  (do
                                    (println "Error fetching set details:" error-details)
                                    (state/set-current-set-error (str "Failed to load set details: " error-details))
                                    (state/set-current-set-loading false)
                                    (state/set-current-set-questions-loading false)))))))

    :component-will-unmount
    (fn []
      ;; Clear out the current set data when leaving the page
      (state/set-current-set nil [])
      (state/set-current-set-error nil)
      (state/set-current-set-questions-error nil)
      (state/set-current-set-loading false)
      (state/set-current-set-questions-loading false))

    :reagent-render
    (fn [match] ; match is the route match data
      (let [current-set-state @(state/get-current-set-state)
            details (:details current-set-state)
            set-loading? (:loading? current-set-state)
            set-error (:error current-set-state)]

        [:div {:class "max-w-3xl mx-auto px-4 py-8"}
         [:div {:class "mb-6"}
          [button {:variant :text
                   :start-icon lucide-icons/ArrowLeft
                   :class "hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)] -ml-3"
                   :on-click #(rfe/push-state :zi-study.frontend.core/question-sets)}
           "Back to Question Sets"]]

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