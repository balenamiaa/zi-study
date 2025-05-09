(ns zi-study.frontend.components.questions.cloze-question
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.card :refer [card]]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.tooltip :refer [tooltip]]
            [zi-study.frontend.components.questions.common :as q-common]
            ["lucide-react" :as lucide-icons]))

;; Helper function to ensure consistent keyword keys
(defn- ensure-keyword-keys [value]
  (cond
    (map? value)
    (reduce-kv (fn [m k v]
                (let [new-k (if (keyword? k) k (keyword k))]
                  (assoc m new-k (ensure-keyword-keys v))))
              {} value)
    
    (or (seq? value) (vector? value))
    (mapv ensure-keyword-keys value)
    
    :else value))

(defn blank-input
  "Renders a single blank input in the cloze question"
  []
  (let [input-ref (r/atom nil)
        animation-class (r/atom "")]
    (r/create-class
     {:component-did-update
      (fn [this old-argv]
        (let [[_ old-props] old-argv
              [_ new-props] (r/argv this)]
          ;; Apply animation when status changes
          (when (and (or (:correct? new-props) (:incorrect? new-props))
                     (not (or (:correct? old-props) (:incorrect? old-props))))
            (reset! animation-class "cloze-blank-animated")
            (js/setTimeout #(reset! animation-class "") 300))))

      :reagent-render
      (fn [{:keys [value index placeholder on-change correct? incorrect? pending? actual-answer submitted? disabled?]}]
        [:div {:class (str "inline-flex items-center justify-center relative mx-0.5 align-bottom transition-all duration-300 "
                           (cond
                             pending? "animate-pulse"
                             correct? "bg-green-50 dark:bg-green-900/10"
                             incorrect? "bg-red-50 dark:bg-red-900/10"
                             :else "")
                           " " @animation-class)}
         
         ;; Use tooltip for incorrect answers when submitted
         (if (and submitted? incorrect? actual-answer)
           [tooltip {:content [:div {:class "p-1"}
                              [:div {:class "text-center mb-1 font-medium text-xs"}
                               "Correct Answer"]
                              [:div {:class "bg-[rgba(255,255,255,0.1)] rounded-md px-2 py-1 text-center font-medium text-xs"}
                               actual-answer]]
                    :position :bottom
                    :delay 100
                    :variant :dark
                    :max-width "120px"
                    :class "text-white"}
            [:input {:type "text"
                    :value value
                    :disabled (or pending? disabled?)
                    :placeholder placeholder
                    :on-focus #(.select (.-target %))
                    :ref #(reset! input-ref %)
                    :on-change #(when on-change
                                  (on-change index (.. % -target -value)))
                    :class (str "w-20 fit-content py-0.5 text-center border-b-2 bg-transparent outline-none transition-all duration-300 text-xs sm:text-sm "
                                (cond
                                  pending? "border-[var(--color-primary)] dark:border-[var(--color-primary-300)]"
                                  correct? "border-[var(--color-success)] dark:border-[var(--color-success-300)]"
                                  incorrect? "border-[var(--color-error)] dark:border-[var(--color-error-300)]"
                                  :else "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] focus:border-[var(--color-primary)] dark:focus:border-[var(--color-primary-300)]"))}]]
           
           ;; Regular input without tooltip
           [:input {:type "text"
                    :value value
                    :disabled (or pending? disabled?)
                    :placeholder placeholder
                    :on-focus #(.select (.-target %))
                    :ref #(reset! input-ref %)
                    :on-change #(when on-change
                                  (on-change index (.. % -target -value)))
                    :class (str "w-20 fit-content py-0.5 text-center border-b-2 bg-transparent outline-none transition-all duration-300 text-xs sm:text-sm"
                                (cond
                                  pending? "border-[var(--color-primary)] dark:border-[var(--color-primary-300)]"
                                  correct? "border-[var(--color-success)] dark:border-[var(--color-success-300)]"
                                  incorrect? "border-[var(--color-error)] dark:border-[var(--color-error-300)]"
                                  :else "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] focus:border-[var(--color-primary)] dark:focus:border-[var(--color-primary-300)]"))}])])})))

(defn parse-cloze-text
  "Parses the cloze text into segments with blanks marked for rendering.
   Returns a vector of segments, where each segment is either a string or a blank identifier."
  [text]
  (if-not (string? text)
    []
    (let [segments (atom [])
          current-pos (atom 0)
          pattern #"\{\{(c\d+)(?:::(.*?))?\}\}"] ;; Match {{c1::answer}} or {{c1}}

      ;; Create a safer way to find all matches without potential infinite loops
      (while (< @current-pos (count text))
        (if-let [match (re-find pattern (subs text @current-pos))]
          (let [match-index (.indexOf (subs text @current-pos) (first match))
                abs-match-index (+ @current-pos match-index)
                blank-id (keyword (second match))] ;; Convert c1 to :c1 keyword

            ;; Add text before the match, if any
            (when (> match-index 0)
              (swap! segments conj (subs text @current-pos (+ @current-pos match-index))))

            ;; Add the blank
            (swap! segments conj {:blank blank-id})

            ;; Update position to after this match
            (reset! current-pos (+ abs-match-index (count (first match)))))

          ;; No more matches, add remaining text
          (do
            (when (< @current-pos (count text))
              (swap! segments conj (subs text @current-pos)))
            (reset! current-pos (count text)))))

      @segments)))

(defn progress-section [correct-count blank-count]
  (let [percentage (if (pos? blank-count)
                     (* (/ correct-count blank-count) 100)
                     0)
        status-color (cond
                       (= correct-count blank-count) "text-[var(--color-success)]"
                       (= correct-count 0) "text-[var(--color-error)]"
                       :else "text-[var(--color-primary)]")]
    [:div {:class "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-lg p-2 mb-3 shadow-sm"}
      [:div {:class "flex items-center justify-between mb-1.5"}
       [:div {:class "flex items-center gap-1.5"}
        [:> (cond
              (= correct-count blank-count) lucide-icons/CheckCircle
              (= correct-count 0) lucide-icons/XCircle
              :else lucide-icons/CircleDot)
         {:size 16
          :className status-color}]
        [:span {:class (str "font-medium text-sm " status-color)}
         (str correct-count "/" blank-count " correct")]]
       
       [:span {:class "text-xs font-medium bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] px-1.5 py-0.5 rounded-full"}
        (str (Math/round percentage) "%")]]
      
      ;; Enhanced progress bar
      [:div {:class "h-2 bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] rounded-full overflow-hidden"}
       [:div {:class (str "h-full transition-all duration-700 ease-out rounded-full "
                          (cond
                            (= correct-count blank-count) "bg-[var(--color-success)]"
                            (= correct-count 0) "bg-[var(--color-error)]"
                            :else "bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary)]"))
              :style {:width (str percentage "%")}}]]]))

(defn cloze-question []
  (let [answers (r/atom {}) ; Map of blank-id -> user input
        show-explanation? (r/atom false)
        processed-segments (r/atom [])
        blank-count (r/atom 0)
        correct-count (r/atom 0)
        submitted-flag (r/atom false)
        animation-trigger (r/atom 0)]

    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [props (r/props this)
              ;; Ensure we have keyword keys
              initial-answers (ensure-keyword-keys (get-in props [:user-answer :answer-data :answers] {}))
              cloze-text (get-in props [:question-data :cloze_text])
              segments (parse-cloze-text cloze-text)
              blanks (->> segments
                          (filter map?)
                          (map :blank))]

          (reset! processed-segments segments)
          (reset! blank-count (count blanks))

          (reset! answers (into {} (map (fn [blank-id]
                                          [blank-id (get initial-answers blank-id "")])
                                        blanks)))

          ;; If we have submitted answers, calculate correct count
          (when-let [correct-answers (get-in props [:question-data :answers])]
            (when (and (vector? correct-answers) (seq correct-answers))
              (let [blank-answers (zipmap blanks correct-answers)
                    ;; Ensure submitted answers use keyword keys
                    submitted-answers (ensure-keyword-keys (get-in props [:user-answer :answer-data :answers] {}))
                    correct-count-val (reduce (fn [count [blank-id _]]
                                                (let [user-answer (get submitted-answers blank-id "")
                                                      correct-answer (get blank-answers blank-id)
                                                      correct? (and correct-answer
                                                                    (= (str/trim (str/lower-case user-answer))
                                                                       (str/trim (str/lower-case correct-answer))))]
                                                  (if correct? (inc count) count)))
                                              0
                                              submitted-answers)]
                (reset! correct-count correct-count-val)
                (reset! submitted-flag true))))))

      :component-did-update
      (fn [this [_ old-props]]
        (let [new-props (r/props this)
              ;; Ensure consistent keyword keys for comparison
              old-answer (ensure-keyword-keys (get-in old-props [:user-answer :answer-data :answers] {}))
              new-answer (ensure-keyword-keys (get-in new-props [:user-answer :answer-data :answers] {}))]

          ;; Check if answers have changed from the server
          (when (not= old-answer new-answer)
            (let [blanks (->> @processed-segments
                              (filter map?)
                              (map :blank))
                  correct-answers (get-in new-props [:question-data :answers])]

              ;; Update answers from server response, ensuring keyword keys
              (reset! answers (into {} (map (fn [blank-id]
                                              [blank-id (get new-answer blank-id "")])
                                            blanks)))

              ;; Update correct count if we have server answers
              (when (and (vector? correct-answers) (seq correct-answers) (seq new-answer))
                (let [blank-answers (zipmap blanks correct-answers)
                      correct-count-val (reduce (fn [count [blank-id user-answer]]
                                                  (let [correct-answer (get blank-answers blank-id)
                                                        correct? (and correct-answer
                                                                      (= (str/trim (str/lower-case user-answer))
                                                                         (str/trim (str/lower-case correct-answer))))]
                                                    (if correct? (inc count) count)))
                                                0
                                                new-answer)]
                  (reset! correct-count correct-count-val)
                  (reset! submitted-flag true)
                  (swap! animation-trigger inc)))))))

      :reagent-render
      (fn [props]
        (let [{:keys [question-id question-data user-answer retention-aid bookmarked index]} props

              ;; State from global app-state (reagent reactions)
              submission-state (q-common/get-deref-answer-submission-state question-id)

              answer-submitting? (:loading? submission-state)
              correct-answers (:answers question-data)
              ;; Ensure server response uses keyword keys
              submitted-answers (ensure-keyword-keys (get-in user-answer [:answer-data :answers] {}))
              submitted? (boolean (and user-answer (seq submitted-answers)))
              is-correct? (and submitted? (= @correct-count @blank-count))
              explanation (:explanation question-data)
              current-segments @processed-segments

              ;; Handle input change
              handle-input-change (fn [blank-id value]
                                    (swap! answers assoc blank-id value))

              ;; Handle answer submission - using keyword keys for answer data
              handle-submit (fn []
                              (reset! submitted-flag true)
                              (http/submit-answer question-id "cloze" {:answers @answers}
                                                  (fn [_result] nil)))

              ;; Check if any blanks are empty
              all-blanks-filled? (every? (fn [[_ value]] (not (str/blank? value))) @answers)

              ;; Clear answers handler
              clear-fn (fn [callback]
                         (reset! submitted-flag false)
                         (http/delete-answer question-id callback)
                         ;; Reset local state
                         (doseq [blank-id (keys @answers)]
                           (swap! answers assoc blank-id ""))
                         (reset! correct-count 0))

              ;; Map of blank IDs to correct answers
              blank-answers (when (and (vector? correct-answers) (seq correct-answers))
                              (let [blanks (->> current-segments
                                                (filter map?)
                                                (map :blank))]
                                (zipmap blanks correct-answers)))
              
              ;; Create a map to track blank number by index
              blank-indices (reduce (fn [acc segment]
                                     (if (map? segment)
                                       (assoc acc (:blank segment) (inc (count acc)))
                                       acc))
                                   {}
                                   current-segments)]

          [card {:class "mb-6" :variant :outlined}
           ;; Question Header - don't show text as we'll display it in the main content
           [q-common/question-header {:index (inc index)
                                      :question-id question-id
                                      :text "" ;; Don't show redundant text in header
                                      :retention-aid (when submitted? retention-aid)
                                      :submitted? submitted?
                                      :is-correct? is-correct?
                                      :bookmarked bookmarked
                                      :clear-fn clear-fn}]

           [:div {:class "px-2.5 pb-2.5"}
            ;; Show answer progress if submitted
            (when (and submitted? @submitted-flag (pos? @blank-count))
              [progress-section @correct-count @blank-count])

            ;; The cloze passage with blanks
            [:div {:class "text-sm sm:text-base leading-relaxed p-2.5 mb-3 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-md" :style {:line-height "2"}}
             (doall
              (map-indexed
               (fn [idx segment]
                 (if (string? segment)
                   ;; Regular text
                   ^{:key (str "text-" idx)}
                   [:span segment]

                   ;; A blank to fill
                   (let [blank-id (:blank segment)
                         blank-num (get blank-indices blank-id)
                         value (get @answers blank-id "")
                         correct-answer (get blank-answers blank-id)
                         user-submitted-answer (get submitted-answers blank-id)
                         correct? (and submitted? correct-answer user-submitted-answer
                                       (= (str/trim (str/lower-case user-submitted-answer))
                                          (str/trim (str/lower-case correct-answer))))
                         incorrect? (and submitted? (not correct?))]
                     ^{:key (str "blank-" idx)}
                     [blank-input {:value value
                                   :index blank-id
                                   :placeholder (str "Blank " blank-num)
                                   :on-change handle-input-change
                                   :correct? correct?
                                   :incorrect? incorrect?
                                   :pending? (and (not submitted?) answer-submitting?)
                                   :actual-answer correct-answer
                                   :submitted? submitted?
                                   :disabled? submitted?}])))
               current-segments))]

            ;; Submit button (only show if not submitted)
            (when (and (not submitted?) (pos? @blank-count))
              [:div {:class "flex justify-end mt-3"}
               [button {:variant :primary
                        :disabled (or answer-submitting? (not all-blanks-filled?))
                        :loading answer-submitting?
                        :size :sm
                        :on-click handle-submit}
                "Submit"]])

            ;; Explanation section
            (when (and explanation submitted?)
              [q-common/explanation-section {:explanation explanation
                                             :show-explanation? @show-explanation?
                                             :on-toggle #(swap! show-explanation? not)}])]]))})))