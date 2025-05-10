(ns zi-study.frontend.components.questions.true-false-question
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.questions.common :as q-common]
            ["lucide-react" :as lucide-icons]))

(defn true-false-question []
  (let [selected-answer-atom (r/atom nil) ; Can be true, false, or nil (not yet selected/submitted)
        show-explanation? (r/atom false)
        submitting-via-local-flag? (r/atom false)] ; To track submission initiated by this component
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [[props] (r/argv this)
              initial-submitted-answer (get-in (:user-answer props) [:answer-data :answer])]
          (reset! selected-answer-atom initial-submitted-answer)))

      :component-did-update
      (fn [this [_ old-props]]
        (let [[new-props] (r/argv this)
              old-submitted-answer (get-in (:user-answer old-props) [:answer-data :answer])
              new-submitted-answer (get-in (:user-answer new-props) [:answer-data :answer])]
          (when (not= old-submitted-answer new-submitted-answer)
            (when-not @submitting-via-local-flag?
              (reset! selected-answer-atom new-submitted-answer)))))

      :reagent-render
      (fn [props]
        (let [{:keys [question-id question-data user-answer retention-aid bookmarked index]} props

              submission-state-from-global (q-common/get-deref-answer-submission-state question-id)

              is-globally-answered? (boolean user-answer)
              globally-submitted-answer (get-in user-answer [:answer-data :answer])
              is-globally-correct? (when is-globally-answered? (= 1 (:is-correct user-answer)))

              is-submission-pending-globally? (or (:loading? submission-state-from-global) @submitting-via-local-flag?)

              text (:text question-data)
              explanation (:explanation question-data)
              ;; For true-false, question-data might store {:is_correct_true true/false} from the import
              ;; but the backend determines correctness, so we rely on user-answer.

              handle-selection (fn [selection]
                                 (when-not is-submission-pending-globally?
                                   (reset! selected-answer-atom selection)
                                   (reset! submitting-via-local-flag? true)
                                   (http/submit-answer question-id "true-false" {:answer selection}
                                                       (fn [_result]
                                                         (reset! submitting-via-local-flag? false)
                                                         ;; user-answer in props will update via state change
                                                         ))))

              clear-fn (fn [callback]
                         (http/delete-answer question-id callback)
                         (reset! selected-answer-atom nil))]

          [card {:class "mb-8" :variant :outlined}
           [q-common/question-header {:index (inc index)
                                      :question-id question-id
                                      :text text
                                      :retention-aid retention-aid
                                      :submitted? is-globally-answered?
                                      :is-correct? is-globally-correct?
                                      :bookmarked bookmarked
                                      :clear-fn clear-fn}]

           [:div {:class "px-5 pb-5 pt-3"}
            (if is-globally-answered?
              ;; Display submitted answer and correctness
              [:div {:class (str "p-3 rounded-md "
                                 (if is-globally-correct?
                                   "bg-[var(--color-success-50)] dark:bg-[rgba(var(--color-success-rgb),0.1)] text-[var(--color-success)]"
                                   "bg-[var(--color-error-50)] dark:bg-[rgba(var(--color-error-rgb),0.1)] text-[var(--color-error)]"))}
               [:div {:class "flex items-center font-medium"}
                (if is-globally-correct?
                  [:> lucide-icons/CheckCircle {:size 20 :className "mr-2"}]
                  [:> lucide-icons/XCircle {:size 20 :className "mr-2"}])
                (str "You answered: " (if globally-submitted-answer "True" "False") ". "
                     (if is-globally-correct? "Correct!" "Incorrect."))]]
              ;; Display selection buttons
              [:div {:class "flex justify-center gap-2"}
               [button {:variant (if (and (some? @selected-answer-atom) @selected-answer-atom (not is-submission-pending-globally?)) :primary :outlined)
                        :color (cond
                                 (and is-submission-pending-globally? @selected-answer-atom) :default ;; Neutral during pending if selected
                                 :else :default)
                        :class "flex-1 max-w-xs"
                        :disabled is-submission-pending-globally?
                        :loading (and is-submission-pending-globally? @selected-answer-atom)
                        :on-click #(handle-selection true)}
                "True"]
               [button {:variant (if (and (some? @selected-answer-atom) (not @selected-answer-atom) (not is-submission-pending-globally?)) :primary :outlined)
                        :color (cond
                                 (and is-submission-pending-globally? (not (nil? @selected-answer-atom)) (not @selected-answer-atom)) :default
                                 :else :default)
                        :class "flex-1 max-w-xs"
                        :disabled is-submission-pending-globally?
                        :loading (and is-submission-pending-globally? (not (nil? @selected-answer-atom)) (not @selected-answer-atom))
                        :on-click #(handle-selection false)}
                "False"]])

            (when (and is-globally-answered? (not is-submission-pending-globally?) explanation)
              [q-common/explanation-section {:explanation explanation
                                             :rx-show-explanation? show-explanation?
                                             :on-toggle #(swap! show-explanation? not)
                                             :question-id question-id}])]]))})))
