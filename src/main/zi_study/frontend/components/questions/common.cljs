(ns zi-study.frontend.components.questions.common
  (:require [reagent.core :as r]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.button :refer [button]]
            ["lucide-react" :as lucide-icons]
            [clojure.string :as str]
            ["marked" :as marked]))


;; Helper to get and dereference the answer submission state reaction
(defn get-deref-answer-submission-state [question-id]
  @(state/get-answer-submission-state question-id))

;; Helper to get and dereference the self-eval state reaction
(defn get-deref-self-eval-state [question-id]
  @(state/get-self-eval-state question-id))

(defn- bookmark-toggle-loading? [question-id]
  (:loading? @(state/get-bookmark-toggle-state question-id)))

(defn bookmark-button [{:keys [question-id bookmarked]}]
  (let [loading? (bookmark-toggle-loading? question-id)]
    [:div {:class (str "cursor-pointer inline-flex items-center justify-center w-8 h-8 rounded-full transition-all flex-shrink-0 "
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
        {:size 28 ; Slightly smaller icon
         :className (if bookmarked "text-[var(--color-primary)]" "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]")}])]))

(defn question-number-badge [number answered? correct?]
  [:div {:class (str "w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center font-medium text-sm mr-3 "
                     (cond
                       (and answered? correct?) "bg-[var(--color-success-100)] text-[var(--color-success)] dark:bg-[rgba(var(--color-success-rgb),0.2)]"
                       answered? "bg-[var(--color-error-100)] text-[var(--color-error)] dark:bg-[rgba(var(--color-error-rgb),0.2)]"
                       :else "bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"))}
   number])

(defn slanted-question-badge [number answered? correct?]
  (let [base-color (cond
                     (and answered? correct?) "bg-[var(--color-success-100)] text-[var(--color-success)] dark:bg-[rgba(var(--color-success-rgb),0.2)]"
                     answered? "bg-[var(--color-error-100)] text-[var(--color-error)] dark:bg-[rgba(var(--color-error-rgb),0.2)]"
                     :else "bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]")
        accent-color (cond
                       (and answered? correct?) "bg-[var(--color-success-50)] dark:bg-[rgba(var(--color-success-rgb),0.1)]"
                       answered? "bg-[var(--color-error-50)] dark:bg-[rgba(var(--color-error-rgb),0.1)]"
                       :else "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]")]
    [:div {:class "flex items-center relative h-8 mr-3"}
     ;; Circle with number
     [:div {:class (str "w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center font-medium text-sm z-10 " base-color)}
      number]

     ;; Extended background with slanted edge
     [:div {:class (str "absolute left-4 h-8 w-12 " accent-color)}]
     ;; Slanted edge shape using clip-path
     [:div {:class (str "absolute left-16 h-8 w-4 " accent-color)
            :style {:clip-path "polygon(0 0, 0% 100%, 100% 100%)"}}]]))

(defn retention-hint [{:keys [text]}]
  (when text
    [:div {:class "mt-3 px-4 py-3 rounded-md bg-[var(--color-secondary-50)] dark:bg-[rgba(var(--color-secondary-rgb),0.1)] flex items-start"}
     [:> lucide-icons/Lightbulb {:size 18 :className "text-[var(--color-secondary)] mt-0.5 mr-2 flex-shrink-0"}]
     [:span {:class "text-sm text-[var(--color-secondary-800)] dark:text-[var(--color-secondary-200)] flex-grow"} text]]))

(def marked-options
  (clj->js
   {:gfm true
    :breaks true
    :sanitize false
    :smartLists true
    :smartypants true
    :xhtml true}))

(defn explanation-section [{:keys [_explanation explanation-id]}]
  (let [container-ref (r/atom nil)
        container-id-str (or explanation-id (str "explanation-" (random-uuid)))]

    (r/create-class
     {:display-name "explanation-section"

      :reagent-render
      (fn [{:keys [explanation rx-show-explanation? on-toggle disabled?]}]
        (let [show-explanation? @rx-show-explanation?]
          [:div {:class "pt-2 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
           [:div {:class "flex justify-center"}
            [button {:variant :outlined
                     :size :xs
                     :class "w-full max-w-xs px-2 py-1 text-xs"
                     :disabled disabled?
                     :start-icon (if show-explanation? lucide-icons/EyeOff lucide-icons/Eye)
                     :on-click on-toggle}
             (if show-explanation? "Hide Explanation" "Show Explanation")]]

           [:div {:id container-id-str
                  :ref (fn [el] (reset! container-ref el))
                  :class (str "mt-1 p-2.5 bg-[var(--color-info-50)] dark:bg-[rgba(var(--color-info-rgb),0.1)] rounded-md"
                              (if show-explanation?
                                "opacity-100 max-h-[2000px]"
                                "opacity-0 max-h-0 h-0 m-0 p-0 overflow-hidden pointer-events-none absolute"))}
            [:div {:class "prose prose-sm dark:prose-invert prose-p:text-[var(--color-light-text-secondary)] dark:prose-p:text-[var(--color-dark-text-secondary)] prose-a:text-[var(--color-primary)] prose-headings:text-[var(--color-light-text)] dark:prose-headings:text-[var(--color-dark-text)] prose-headings:font-medium prose-img:rounded-md prose-pre:bg-[var(--color-light-bg)] dark:prose-pre:bg-[var(--color-dark-bg)] prose-pre:text-sm prose-code:text-[var(--color-primary-700)] dark:prose-code:text-[var(--color-primary-300)] prose-code:bg-[var(--color-primary-50)] dark:prose-code:bg-[rgba(var(--color-primary-rgb),0.1)] prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-sm prose-strong:text-[var(--color-light-text)] dark:prose-strong:text-[var(--color-dark-text)] prose-li:marker:text-[var(--color-light-text-secondary)] dark:prose-li:marker:text-[var(--color-dark-text-secondary)] max-w-none"}
             (let [cleaned-explanation (str/replace (str explanation) #"(?m)^ {14}" "")]
               [:div {:dangerouslySetInnerHTML (r/unsafe-html (marked/parse cleaned-explanation marked-options))}])]]]))})))

(defn question-header
  "Common header component for questions"
  [{:keys [index question-id text retention-aid submitted? is-correct? bookmarked clear-fn]}]
  (let [clearing (r/atom false)
        has-text? (not (str/blank? text))]
    [:div {:class "p-3 sm:p-4"}
     ;; Top row with question number, text, and bookmark/clear buttons
     [:div {:class (str "flex items-start justify-between gap-2 sm:gap-3 "
                        (when-not has-text? "pb-1"))}
      ;; If there's no text, create a more compact and visual badge
      (if has-text?
        ;; With text - standard layout
        [:div {:class "flex items-start flex-grow min-w-0"}
         [question-number-badge index submitted? is-correct?]
         [:div {:class "min-w-0 flex-grow"} ; min-w-0 allows text to truncate properly
          [:div {:class "font-medium text-base sm:text-lg mb-0.5 break-words"} text]]]

        ;; Without text - stylish badge with extended shape
        [:div {:class "flex items-center h-8"} ; Set height to match badge
         [slanted-question-badge index submitted? is-correct?]])

      ;; Bookmark and clear buttons - vertical layout
      [:div {:class "flex-shrink-0 ml-auto flex flex-col gap-1"}
       ;; Bookmark button
       [bookmark-button {:question-id question-id
                         :bookmarked bookmarked}]

       ;; Clear button - always visible but disabled unless submitted
       [:div {:class (str "inline-flex items-center justify-center w-8 h-8 rounded-full transition-all flex-shrink-0 "
                          (if @clearing
                            "bg-[var(--color-error-100)] dark:bg-[rgba(var(--color-error-rgb),0.2)]"
                            (if submitted?
                              "cursor-pointer hover:bg-[var(--color-error-50)] dark:hover:bg-[rgba(var(--color-error-rgb),0.1)]"
                              "opacity-40 cursor-default")))}
        [:div {:on-click (fn []
                           (when (and submitted? (not @clearing))
                             (reset! clearing true)
                             (clear-fn (fn [_] (reset! clearing false)))))}
         (if @clearing
           [:> lucide-icons/Loader2 {:size 20 :className "animate-spin text-[var(--color-error)]"}]
           [:> lucide-icons/Trash2 {:size 20 :className "text-[var(--color-error)]"}])]]]]

     ;; Middle row - retention aid (full width on mobile)
     (when (and submitted? retention-aid)
       [retention-hint {:text retention-aid}])]))
