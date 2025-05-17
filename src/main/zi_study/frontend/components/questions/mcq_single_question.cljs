(ns zi-study.frontend.components.questions.mcq-single-question
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.questions.common :as q-common]
            ["lucide-react" :as lucide-icons]))

(defn mcq-single-option
  "A single MCQ option component"
  [{:keys [option is-selected is-correct is-actually-correct answered-globally pending-globally on-click]}]
  [:div {:class (str "mb-3 p-3 rounded-md border-2 transition-all flex items-center "
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
              submission-state-from-global (q-common/get-deref-answer-submission-state question-id)

              ;; Has any answer been successfully submitted and recorded in user-answer?
              is-globally-answered? (boolean user-answer)
              ;; What was the index of the globally confirmed answer?
              globally-submitted-idx (get-in user-answer [:answer-data :answer])

              ;; Is a submission currently in progress (either globally or initiated locally)?
              is-submission-pending-globally? (or (:loading? submission-state-from-global) @submitting-via-local-flag?)

              text (:text question-data)
              options (:options question-data)
              actual-correct-idx (:correct-index question-data)
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
           [q-common/question-header {:index (inc index)
                                      :question-id question-id
                                      :text text
                                      :retention-aid retention-aid
                                      :submitted? is-globally-answered?
                                      :is-correct? (when is-globally-answered? (= actual-correct-idx globally-submitted-idx))
                                      :bookmarked bookmarked
                                      :clear-fn clear-fn}]

           [:div {:class "px-3 pb-3"}
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

           (when (and is-globally-answered? explanation)
             [q-common/explanation-section {:explanation explanation
                                            :rx-show-explanation? show-explanation?
                                            :on-toggle #(swap! show-explanation? not)
                                            :question-id question-id}])]))})))