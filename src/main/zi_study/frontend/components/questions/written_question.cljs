(ns zi-study.frontend.components.questions.written-question
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.questions.common :as q-common]
            ["lucide-react" :as lucide-icons]))


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
              submission-state (q-common/get-deref-answer-submission-state question-id)
              self-eval-state (q-common/get-deref-self-eval-state question-id)
              submitted? (boolean user-answer)
              self-evaluated? (and submitted? (some? (:is-correct user-answer)))
              is-correct? (= 1 (:is-correct user-answer))
              text (:text question-data)
              correct-answer (:correct_answer question-data)
              explanation (:explanation question-data)
              answer-loading? (:loading? submission-state)
              eval-loading? (:loading? self-eval-state)]

          [card {:class "mb-8" :variant :outlined}
           [q-common/question-header {:index (inc index)
                                      :question-id question-id
                                      :text text
                                      :retention-aid retention-aid
                                      :submitted? submitted?
                                      :is-correct? is-correct?
                                      :bookmarked bookmarked
                                      :clear-fn (fn [callback]
                                                  (http/delete-answer question-id callback))}]

           [:div {:class "p-3 pt-0"}
            (if (and submitted? (not self-evaluated?))
              [:div
               [:div {:class "mb-4 p-3 bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] rounded-md"}
                [:div {:class "font-medium mb-2"} "Your answer:"]
                [:p (get-in user-answer [:answer-data :answer])]]

               [:div {:class "mb-4 p-4 bg-[var(--color-primary-50)] dark:bg-[rgba(var(--color-primary-rgb),0.1)] rounded-md"}
                [:div {:class "font-medium mb-2 text-[var(--color-primary)]"} "Correct answer:"]
                [:p correct-answer]]

               (when explanation
                 [q-common/explanation-section {:explanation explanation
                                                :rx-show-explanation? show-explanation?
                                                :on-toggle #(swap! show-explanation? not)
                                                :question-id question-id}])

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
                    [q-common/explanation-section {:explanation explanation
                                                   :rx-show-explanation? show-explanation?
                                                   :on-toggle #(swap! show-explanation? not)
                                                   :question-id question-id}])])

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
