(ns zi-study.frontend.components.questions.common
  (:require [reagent.core :as r]
            [zi-study.frontend.state :as state]
            [zi-study.frontend.utilities.http :as http]
            [zi-study.frontend.components.button :refer [button]]
            ["lucide-react" :as lucide-icons]))


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
        {:size 32
         :className (if bookmarked "text-[var(--color-primary)]" "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]")}])]))

(defn question-number-badge [number answered? correct?]
  [:div {:class (str "w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center font-medium text-sm mr-3 "
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
