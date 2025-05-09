(ns zi-study.frontend.components.questions.mcq-multi-question
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.questions.common :as q-common]
            ["lucide-react" :as lucide-icons]))

(defn- mcq-multi-option
  [{:keys [option-text option-idx selected? actual-correct? globally-answered? pending-globally? on-click]}]
  [:div {:class (str "mb-3 p-3 rounded-md border-2 transition-all flex items-center "
                     (if pending-globally? "cursor-wait" "cursor-pointer ")
                     (cond
                       ;; This option is part of a pending submission and is selected
                       (and selected? pending-globally?)
                       "border-[var(--color-primary)] bg-[rgba(var(--color-primary-rgb),0.05)] animate-pulse"

                       ;; This option was submitted and is part of the correct answer set
                       (and globally-answered? selected? actual-correct?)
                       "border-[var(--color-success)] bg-[var(--color-success-50)] dark:bg-[rgba(var(--color-success-rgb),0.1)]"

                       ;; This option was submitted, selected by user, but is NOT part of the correct answer set
                       (and globally-answered? selected? (not actual-correct?))
                       "border-[var(--color-error)] bg-[var(--color-error-50)] dark:bg-[rgba(var(--color-error-rgb),0.1)]"

                       ;; This option is selected by the user for a new/changed answer, but not yet submitted
                       (and selected? (not globally-answered?) (not pending-globally?))
                       "border-[var(--color-primary-300)] dark:border-[var(--color-primary-400)]"

                       ;; Default state or if another option is selected
                       :else
                       "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] hover:border-[var(--color-primary-300)] dark:hover:border-[var(--color-primary-400)]"))
         :on-click (when (not pending-globally?) #(on-click option-idx))}

   [:div {:class (str "w-6 h-6 rounded flex-shrink-0 flex items-center justify-center mr-3 border "
                      (cond
                        (and selected? pending-globally?) "border-[var(--color-primary)]"
                        (and globally-answered? selected? actual-correct?) "border-[var(--color-success)] bg-[var(--color-success-100)] dark:bg-[rgba(var(--color-success-rgb),0.2)]"
                        (and globally-answered? selected? (not actual-correct?)) "border-[var(--color-error)] bg-[var(--color-error-100)] dark:bg-[rgba(var(--color-error-rgb),0.2)]"
                        selected? "border-[var(--color-primary)] bg-[var(--color-primary-100)] dark:bg-[rgba(var(--color-primary-rgb),0.2)]"
                        :else "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"))}
    (cond
      (and selected? pending-globally?)
      [:> lucide-icons/Loader2 {:size 16 :className "text-[var(--color-primary)] animate-spin"}]

      selected? ; Show check if selected (covers submitted correct/incorrect, and pre-submission selection)
      [:> lucide-icons/Check {:size 16 :className (cond
                                                    (and globally-answered? actual-correct?) "text-[var(--color-success)]"
                                                    (and globally-answered? (not actual-correct?)) "text-[var(--color-error)]"
                                                    :else "text-[var(--color-primary)]")}]
      :else nil)]

   [:div {:class (str "flex-grow "
                      (when (and selected? pending-globally?) "text-[var(--color-primary)]")
                      (when (and globally-answered? selected? actual-correct?) "text-[var(--color-success-700)] dark:text-[var(--color-success-300)]")
                      (when (and globally-answered? selected? (not actual-correct?)) "text-[var(--color-error-700)] dark:text-[var(--color-error-300)]"))}
    option-text]

   ;; For MCQ-Multi, after submission, we want to indicate all actual correct answers, even if not selected by user.
   (when (and globally-answered? (not pending-globally?) actual-correct? (not selected?))
     [:> lucide-icons/CheckSquare {:size 20 :className "ml-auto text-[var(--color-success)] opacity-70" :title "This was a correct option"}])])

(defn mcq-multi-question []
  (let [selected-indices-atom (r/atom []) ; Store as vector instead of set
        show-explanation? (r/atom false)
        submitting-via-local-flag? (r/atom false)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [[props] (r/argv this)
              initial-submitted-answer (get-in (:user-answer props) [:answer-data :answer] [])]
          (reset! selected-indices-atom (cond
                                          (vector? initial-submitted-answer) initial-submitted-answer
                                          (set? initial-submitted-answer) (vec initial-submitted-answer)
                                          :else []))))

      :component-did-update
      (fn [this [_ old-props]]
        (let [[new-props] (r/argv this)
              old-submitted-answer (get-in (:user-answer old-props) [:answer-data :answer] ::not-found)
              new-submitted-answer (get-in (:user-answer new-props) [:answer-data :answer] ::not-found)]
          (when (not= old-submitted-answer new-submitted-answer)
            (when-not @submitting-via-local-flag?
              (reset! selected-indices-atom (cond
                                              (and (not= new-submitted-answer ::not-found) (vector? new-submitted-answer))
                                              new-submitted-answer
                                              
                                              (and (not= new-submitted-answer ::not-found) (set? new-submitted-answer))
                                              (vec new-submitted-answer)
                                              
                                              :else []))))))
      :reagent-render
      (fn [props]
        (let [{:keys [question-id question-data user-answer retention-aid bookmarked index]} props
              submission-state-from-global (q-common/get-deref-answer-submission-state question-id)

              is-globally-answered? (boolean user-answer)
              globally-submitted-indices (get-in user-answer [:answer-data :answer] [])
              globally-submitted-indices-set (if (set? globally-submitted-indices)
                                              globally-submitted-indices
                                              (set globally-submitted-indices))
              is-globally-correct? (when is-globally-answered? (= 1 (:is-correct user-answer)))

              is-submission-pending-globally? (or (:loading? submission-state-from-global) @submitting-via-local-flag?)

              text (:text question-data)
              options (:options question-data)
              actual-correct-indices-set (set (:correct_indices question-data))
              explanation (:explanation question-data)

              current-selection @selected-indices-atom
              current-selection-set (set current-selection)

              handle-option-toggle (fn [toggled-idx]
                                     (when-not (or is-globally-answered? is-submission-pending-globally?)
                                       (swap! selected-indices-atom
                                              (fn [current-vec]
                                                (if (some #(= % toggled-idx) current-vec)
                                                  (vec (remove #(= % toggled-idx) current-vec))
                                                  (conj current-vec toggled-idx))))))

              handle-submit (fn []
                              (when (and (not is-submission-pending-globally?) (seq current-selection))
                                (reset! submitting-via-local-flag? true)
                                (http/submit-answer question-id "mcq-multi" {:answer current-selection}
                                                    (fn [_result]
                                                      (reset! submitting-via-local-flag? false)
                                                      ;; user-answer in props will update via state change
                                                      ))))
              clear-fn (fn [callback]
                         (http/delete-answer question-id callback)
                         (reset! selected-indices-atom []))]

          [card {:class "mb-8" :variant :outlined}
           [q-common/question-header {:index (inc index)
                                      :question-id question-id
                                      :text text
                                      :retention-aid retention-aid
                                      :submitted? is-globally-answered?
                                      :is-correct? is-globally-correct?
                                      :bookmarked bookmarked
                                      :clear-fn clear-fn}]

           [:div {:class "p-3"}
            (doall
             (for [[idx option-text] (map-indexed vector options)]
               ^{:key idx}
               [mcq-multi-option {:option-text option-text
                                  :option-idx idx
                                  :selected? (if is-globally-answered?
                                               (contains? globally-submitted-indices-set idx)
                                               (contains? current-selection-set idx))
                                  :actual-correct? (contains? actual-correct-indices-set idx)
                                  :globally-answered? is-globally-answered?
                                  :pending-globally? is-submission-pending-globally?
                                  :on-click handle-option-toggle}]))

            (when (and is-globally-answered? is-globally-correct? (not is-submission-pending-globally?))
              [:div {:class "mt-4 p-3 rounded-md bg-[var(--color-success-50)] dark:bg-[rgba(var(--color-success-rgb),0.1)] text-center font-medium text-[var(--color-success)]"}
               [:> lucide-icons/CheckCircle {:size 18 :className "inline mr-2"}] "Correct!"])
            (when (and is-globally-answered? (not is-globally-correct?) (not is-submission-pending-globally?))
              [:div {:class "mt-4 p-3 rounded-md bg-[var(--color-error-50)] dark:bg-[rgba(var(--color-error-rgb),0.1)] text-center font-medium text-[var(--color-error)]"}
               [:> lucide-icons/XCircle {:size 18 :className "inline mr-2"}] "Incorrect."])

            (when (not is-globally-answered?)
              [:div {:class "mt-6 flex justify-end"}
               [button {:variant :primary
                        :disabled (or is-submission-pending-globally? (empty? current-selection))
                        :loading is-submission-pending-globally?
                        :on-click handle-submit}
                "Submit Answer"]])

            (when (and is-globally-answered? (not is-submission-pending-globally?) explanation)
              [q-common/explanation-section {:explanation explanation
                                             :show-explanation? @show-explanation?
                                             :on-toggle #(swap! show-explanation? not)}])]]))})))
