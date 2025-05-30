(ns zi-study.frontend.components.questions.emq-question
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.questions.common :as q-common]
            [zi-study.frontend.components.dropdown :refer [dropdown menu-item]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.tooltip :refer [tooltip]]
            [zi-study.frontend.utilities :refer [cx]]
            ["lucide-react" :as lucide-icons]))

(defn- premise-item
  "Renders a single premise item with a dropdown for selecting an option"
  [{:keys [premise options selected-option-idx disabled? pending? on-selection-change is-submitted? is-correct? actual-correct-option-idx]}]
  [:div {:class (cx "mb-3 transition-all duration-300 rounded-lg p-3 border"
                    (cond
                      pending? "bg-[var(--color-primary-50)] dark:bg-[rgba(233,30,99,0.1)] border-[var(--color-primary-200)] dark:border-[var(--color-primary-700)]"
                      (and is-submitted? is-correct?) "bg-[var(--color-success-50)] dark:bg-[rgba(76,175,80,0.1)] border-[var(--color-success-200)] dark:border-[var(--color-success-700)]"
                      (and is-submitted? (not is-correct?)) "bg-[var(--color-error-50)] dark:bg-[rgba(244,67,54,0.1)] border-[var(--color-error-200)] dark:border-[var(--color-error-700)]"
                      :else "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] dark:bg-[var(--color-dark-bg-paper)] hover:border-[var(--color-primary-300)] dark:hover:border-[var(--color-primary-500)]")
                    "transform transition-all duration-300 hover:shadow-sm")}
   [:div {:class "flex flex-col md:flex-row md:items-center gap-3 items-center"}
    [:div {:class "flex items-start md:w-1/2"}
     [:div {:class (cx "rounded text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"
                       "py-2 px-4 text-left flex items-start justify-start font-bold w-full"
                       "bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)]"
                       "min-h-9 max-h-24 h-auto overflow-y-auto"
                       "scrollbar-thin scrollbar-track-[var(--color-light-bg)] dark:scrollbar-track-[var(--color-dark-bg)]"
                       "scrollbar-thumb-[var(--color-light-divider)] dark:scrollbar-thumb-[var(--color-dark-divider)]"
                       "hover:scrollbar-thumb-[var(--color-primary-300)] dark:hover:scrollbar-thumb-[var(--color-primary-500)]"
                       "scrollbar-thumb-rounded")}
      premise]]

    [:div {:class "flex-grow w-full md:w-1/2"}
     (r/with-let [open? (r/atom false)]
       [dropdown {:trigger
                  [button
                   {:variant (if (and is-submitted? is-correct?)
                               :success
                               (if (and is-submitted? (not is-correct?))
                                 :error
                                 :outlined))
                    :class (cx "justify-between text-left max-w-full mx-auto"
                               "border-2 shadow-sm hover:shadow transition-all")
                    :disabled disabled?
                    :end-icon (cond
                                pending? lucide-icons/Loader2
                                :else lucide-icons/ChevronDown)}
                   [:span {:class (cx "truncate"
                                      (when (nil? selected-option-idx) "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"))}
                    (if (some? selected-option-idx)
                      (get options selected-option-idx)
                      "Select an option...")]]
                  :open? open?
                  :width :match-trigger
                  :transition :scale
                  :trigger-class "flex w-full justify-center"
                  :class (cx "max-h-60 shadow-lg"
                             "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]")}

        (for [[option-idx option-text] (map-indexed vector options)]
          ^{:key (str "option-" option-idx)}
          [menu-item {:on-click #(when-not disabled?
                                   (on-selection-change option-idx)
                                   (reset! open? false))
                      :class (cx (when (= option-idx actual-correct-option-idx) "font-medium")
                                 "hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)]")
                      :end-icon (when (and is-submitted? (= option-idx actual-correct-option-idx)) lucide-icons/Check)}
           [tooltip {:content option-text
                     :position :top
                     :variant :light
                     :delay 100}
            [:span {:class "truncate"} option-text]]])])]]

   [:div {:class (str "transition-all duration-300 transform "
                      (if (and is-submitted? (not is-correct?) (some? actual-correct-option-idx))
                        "mt-2 opacity-100 max-h-20 text-sm p-2 rounded-md bg-[var(--color-success-50)] dark:bg-[rgba(76,175,80,0.1)] text-[var(--color-success-700)] dark:text-[var(--color-success-300)] border border-[var(--color-success-200)] dark:border-[var(--color-success-700)]"
                        "max-h-0 opacity-0 overflow-hidden"))}
    [:div {:class "flex items-center"}
     [:> lucide-icons/AlertCircle {:size 16, :class "flex-shrink-0"}]
     [:span {:class "ml-2 font-medium"}
      (get options actual-correct-option-idx)]]]])

(defn emq-question
  "Extended Matching Questions component
   Displays a list of premises, each with a dropdown to select from a list of options"
  []
  (let [selections-atom (r/atom {})
        show-explanation? (r/atom false)
        submitting? (r/atom false)]

    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [props (r/props this)
              existing-answers (get-in props [:user-answer :answer-data :answers] {})
              normalized-answers (if (sequential? existing-answers)
                                   (into {} existing-answers)
                                   existing-answers)]
          (reset! selections-atom normalized-answers)))

      :component-did-update
      (fn [this [_ old-props]]
        (let [new-props (r/props this)
              old-answers (get-in old-props [:user-answer :answer-data :answers] {})
              new-answers (get-in new-props [:user-answer :answer-data :answers] {})
              normalized-answers (if (sequential? new-answers)
                                   (into {} new-answers)
                                   new-answers)]
          (when (and (not= old-answers new-answers)
                     (not @submitting?))
            (reset! selections-atom normalized-answers))))

      :reagent-render
      (fn [props]
        (let [{:keys [question-id question-data user-answer retention-aid bookmarked index]} props
              instructions (get question-data :instructions "Match each premise with the correct option.")
              premises (get question-data :premises [])
              options (get question-data :options [])
              matches (get question-data :matches [])
              explanation (get question-data :explanation)
              correct-matches (if (map? matches)
                                (into {} (map (fn [[k v]] [(if (keyword? k) (parse-long (name k)) k) v]) matches))
                                (into {} matches))
              is-submitted? (boolean user-answer)
              submitted-answers (let [answers (get-in user-answer [:answer-data :answers])]
                                  (if (map? answers)
                                    answers
                                    (when (sequential? answers)
                                      (into {} answers))))
              submission-state (q-common/get-deref-answer-submission-state question-id)
              pending? (or (:loading? submission-state) @submitting?)
              all-premises-selected? (every? #(contains? @selections-atom %) (range (count premises)))
              handle-selection-change (fn [premise-idx option-idx]
                                        (swap! selections-atom assoc premise-idx option-idx))
              submit-answers (fn []
                               (reset! submitting? true)
                               (http/submit-answer
                                question-id
                                "emq"
                                {:answers (vec (map vec @selections-atom))}
                                (fn [_]
                                  (reset! submitting? false))))
              clear-answers (fn [callback]
                              (http/delete-answer question-id callback)
                              (reset! selections-atom {}))
              is-selection-correct? (fn [premise-idx selection]
                                      (= (get correct-matches premise-idx) selection))
              correct-count (when is-submitted?
                              (count (filter (fn [[premise-idx option-idx]]
                                               (is-selection-correct? premise-idx option-idx))
                                             submitted-answers)))
              total-count (count premises)
              percentage-correct (when is-submitted?
                                   (if (pos? total-count)
                                     (* 100 (/ correct-count total-count))
                                     0))]

          [card {:class "mb-8 overflow-hidden border-2 hover:shadow-md transition-all duration-300" :variant :outlined}
           [q-common/question-header {:index (inc index)
                                      :question-id question-id
                                      :text instructions
                                      :retention-aid retention-aid
                                      :submitted? is-submitted?
                                      :is-correct? (when is-submitted?
                                                     (every? (fn [[premise-idx option-idx]]
                                                               (is-selection-correct? premise-idx option-idx))
                                                             submitted-answers))
                                      :bookmarked bookmarked
                                      :clear-fn clear-answers}]

           [:div {:class "p-2"}
            [:div {:class (str "transition-all duration-300 "
                               (if is-submitted?
                                 "opacity-100 max-h-24 mb-6"
                                 "opacity-0 max-h-0 overflow-hidden"))}
             [:div {:class "px-3 py-2 rounded-lg bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
              [:div {:class "flex justify-between text-sm mb-1"}
               [:span {:class "font-medium"} "Your Score"]
               [:span {:class (cx "font-medium"
                                  (cond
                                    (>= percentage-correct 80) "text-[var(--color-success)]"
                                    (>= percentage-correct 50) "text-[var(--color-warning)]"
                                    :else "text-[var(--color-error)]"))}
                (str correct-count "/" total-count " (" (Math/round percentage-correct) "%)")]]
              [:div {:class "h-2 w-full bg-[var(--color-light-bg-subtle)] dark:bg-[var(--color-dark-bg-subtle)] rounded-full overflow-hidden"}
               [:div {:class (cx "h-full transition-all duration-1000 ease-out"
                                 (cond
                                   (>= percentage-correct 80) "bg-[var(--color-success)]"
                                   (>= percentage-correct 50) "bg-[var(--color-warning)]"
                                   :else "bg-[var(--color-error)]"))
                      :style {:width (str percentage-correct "%")}}]]]]

            (doall
             (for [premise-idx (range (count premises))]
               ^{:key (str "premise-" premise-idx)}
               [premise-item {:index premise-idx
                              :premise (get premises premise-idx)
                              :options options
                              :selected-option-idx (get @selections-atom premise-idx)
                              :disabled? (or pending? is-submitted?)
                              :pending? (and pending? (contains? @selections-atom premise-idx))
                              :on-selection-change #(handle-selection-change premise-idx %)
                              :is-submitted? is-submitted?
                              :is-correct? (when is-submitted?
                                             (is-selection-correct?
                                              premise-idx
                                              (get submitted-answers premise-idx)))
                              :actual-correct-option-idx (when is-submitted?
                                                           (get correct-matches premise-idx))}]))

            [:div {:class (str "transition-all duration-300 "
                               (if (and (not is-submitted?) (not pending?))
                                 "opacity-100 max-h-20 mt-6"
                                 "opacity-0 max-h-0 overflow-hidden"))}
             [:div {:class "flex justify-end"}
              [button
               {:variant :primary
                :disabled (not all-premises-selected?)
                :loading pending?
                :on-click submit-answers
                :class "px-6 py-2 shadow-md hover:shadow-lg transition-all"}
               "Submit Answers"]]]

            (when explanation
              [q-common/explanation-section
               {:explanation-id (str "explanation-" question-id)
                :explanation explanation
                :rx-show-explanation? show-explanation?
                :on-toggle #(swap! show-explanation? not)
                :disabled? (not is-submitted?)
                :question-id question-id}])]]))})))
