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

(defn explanation-section [{:keys [explanation rx-show-explanation? on-toggle]}]
  (let [container-ref (r/atom nil)
        observer-atom (r/atom nil)
        container-id-str (str "explanation-" (or explanation (random-uuid)))]

    (letfn [(manage-observer [{:keys [rx-show-explanation? on-toggle]}]
              (let [show-explanation? @rx-show-explanation?
                    current-container @container-ref]
                (if (and show-explanation? current-container (nil? @observer-atom))
                  (let [obs (js/IntersectionObserver.
                             (fn [entries _observer]
                               (doseq [entry entries]
                                 (when (and (not (.-isIntersecting entry)) @rx-show-explanation?)
                                   (on-toggle))))
                             #js{:root nil :threshold [0.0]})]
                    (.observe obs current-container)
                    (reset! observer-atom obs))
                  (when (and (or (not show-explanation?) (nil? current-container)) (some? @observer-atom))
                    (.disconnect @observer-atom)
                    (reset! observer-atom nil)))))]

      (r/create-class
       {:display-name "explanation-section"

        :component-did-mount
        (fn [this]
          (manage-observer (r/props this)))

        :component-did-update
        (fn [this _old-argv]
          (manage-observer (r/props this)))

        :component-will-unmount
        (fn []
          (when-let [obs @observer-atom]
            (.disconnect obs)
            (reset! observer-atom nil)))

        :reagent-render
        (fn [{:keys [explanation rx-show-explanation? on-toggle]}]
          (let [show-explanation? @rx-show-explanation?]
            [:div {:class "mt-5 pt-4 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
             [:div {:class "flex justify-center mb-2"}
              [button {:variant :outlined
                       :size :sm
                       :class "w-full max-w-xs"
                       :start-icon (if show-explanation? lucide-icons/EyeOff lucide-icons/Eye)
                       :on-click on-toggle}
               (if show-explanation? "Hide Explanation" "Show Explanation")]]

             (when show-explanation?
               [:div {:id container-id-str
                      :ref (fn [el] (reset! container-ref el))
                      :class "mt-3 p-4 bg-[var(--color-info-50)] dark:bg-[rgba(var(--color-info-rgb),0.1)] rounded-md"}
                [:div {:class "prose prose-sm dark:prose-invert prose-p:text-[var(--color-light-text-secondary)] dark:prose-p:text-[var(--color-dark-text-secondary)] prose-a:text-[var(--color-primary)] prose-headings:text-[var(--color-light-text)] dark:prose-headings:text-[var(--color-dark-text)] prose-headings:font-medium prose-img:rounded-md prose-pre:bg-[var(--color-light-bg)] dark:prose-pre:bg-[var(--color-dark-bg)] prose-pre:text-sm prose-code:text-[var(--color-primary-700)] dark:prose-code:text-[var(--color-primary-300)] prose-code:bg-[var(--color-primary-50)] dark:prose-code:bg-[rgba(var(--color-primary-rgb),0.1)] prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-sm prose-strong:text-[var(--color-light-text)] dark:prose-strong:text-[var(--color-dark-text)] prose-li:marker:text-[var(--color-light-text-secondary)] dark:prose-li:marker:text-[var(--color-dark-text-secondary)] max-w-none"}
                 [:div {:dangerouslySetInnerHTML (r/unsafe-html (marked/parse (str explanation) marked-options))}]]])]))}))))

(defn question-header
  "Common header component for questions"
  [{:keys [index question-id text retention-aid submitted? is-correct? bookmarked clear-fn]}]
  (let [clearing (r/atom false)
        has-text? (not (str/blank? text))]
    [:div {:class "p-4 sm:p-5"}
     ;; Top row with question number, text, and bookmark button
     [:div {:class (str "flex items-start justify-between gap-2 sm:gap-4 "
                        (when-not has-text? "pb-1"))}
      ;; If there's no text, create a more compact and visual badge
      (if has-text?
        ;; With text - standard layout
        [:div {:class "flex items-start flex-grow min-w-0"} ; min-w-0 prevents flex children from overflowing
         [question-number-badge index submitted? is-correct?]
         [:div {:class "min-w-0 flex-grow"} ; min-w-0 allows text to truncate properly
          [:div {:class "font-medium text-base sm:text-lg mb-1 break-words"} text]]]

        ;; Without text - stylish badge with extended shape
        [:div {:class "flex items-center h-8"} ; Set height to match badge
         [slanted-question-badge index submitted? is-correct?]])

      ;; Bookmark button - always visible
      [:div {:class "flex-shrink-0 ml-2"}
       [bookmark-button {:question-id question-id
                         :bookmarked bookmarked}]]]

     ;; Middle row - retention aid (full width on mobile)
     (when (and submitted? retention-aid)
       [retention-hint {:text retention-aid}])

     ;; Bottom row - clear button (only when submitted)
     (when submitted?
       [:div {:class "flex justify-end mt-2"}
        [button {:variant :text
                 :size :sm
                 :color :error
                 :loading @clearing
                 :disabled @clearing
                 :start-icon lucide-icons/Trash2
                 :on-click (fn []
                             (reset! clearing true)
                             (clear-fn (fn [_] (reset! clearing false))))}
         "Clear"]])]))
