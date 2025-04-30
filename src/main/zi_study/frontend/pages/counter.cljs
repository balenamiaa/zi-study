(ns zi-study.frontend.pages.counter
  (:require [reagent.core :as r]
            [zi-study.frontend.components.button :refer [button]]
            [zi-study.frontend.components.card :refer [card card-header card-content card-footer]]
            [zi-study.frontend.utilities.storage :as storage]
            ["lucide-react" :as lucide]))

;; Define a local storage key for persisting the counter value
(def COUNTER_STORAGE_KEY "counter-value")

;; Create a counter model to handle counter state and operations
(defonce counter-model
  (let [initial-value (let [saved-value (storage/get-item COUNTER_STORAGE_KEY)]
                        (if saved-value
                          (js/parseInt saved-value 10)
                          0))]
    (r/atom {:value initial-value
             :history []})))

;; Counter operations
(defn increment []
  (swap! counter-model (fn [state]
                         (let [new-value (inc (:value state))]
                           (storage/set-item COUNTER_STORAGE_KEY (str new-value))
                           (-> state
                               (assoc :value new-value)
                               (update :history conj {:action :increment
                                                      :timestamp (js/Date.now)
                                                      :previous-value (:value state)
                                                      :new-value new-value}))))))

(defn decrement []
  (swap! counter-model (fn [state]
                         (let [new-value (dec (:value state))]
                           (storage/set-item COUNTER_STORAGE_KEY (str new-value))
                           (-> state
                               (assoc :value new-value)
                               (update :history conj {:action :decrement
                                                      :timestamp (js/Date.now)
                                                      :previous-value (:value state)
                                                      :new-value new-value}))))))

(defn reset-counter []
  (swap! counter-model (fn [state]
                         (storage/set-item COUNTER_STORAGE_KEY "0")
                         (-> state
                             (assoc :value 0)
                             (update :history conj {:action :reset
                                                    :timestamp (js/Date.now)
                                                    :previous-value (:value state)
                                                    :new-value 0})))))

;; Counter page component
(defn counter-page []
  (fn []
    (let [current-value (:value @counter-model)
          history (:history @counter-model)]
      [:div {:class "container mx-auto px-4 py-8"}
       [:h1 {:class "text-2xl font-bold mb-6 text-center text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
        "Counter Example"]

       [card {:variant :elevated
              :class "max-w-md mx-auto"}

        [card-header
         [:div {:class "flex items-center space-x-2"}
          [:> lucide/Calculator {:size 20}]
          [:h2 {:class "text-lg font-semibold"} "Counter"]]]

        [card-content {}
         [:div {:class "flex flex-col items-center justify-center py-4"}

          ;; Counter display
          [:div {:class (str "text-6xl font-bold mb-8 transition-colors "
                             (cond
                               (pos? current-value) "text-[var(--color-success)]"
                               (neg? current-value) "text-[var(--color-error)]"
                               :else "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"))}
           current-value]

          ;; Counter buttons
          [:div {:class "flex space-x-4"}
           [button {:variant :outlined
                    :start-icon lucide/Minus
                    :on-click decrement}
            "Decrement"]

           [button {:variant :primary
                    :start-icon lucide/Plus
                    :on-click increment}
            "Increment"]]

          ;; Reset button
          [:div {:class "mt-6"}
           [button {:variant :danger
                    :start-icon lucide/RefreshCw
                    :on-click reset-counter
                    :disabled (zero? current-value)}
            "Reset"]]]]

        ;; Optional - show history
        [card-footer {}
         [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
          (if (empty? history)
            "No actions yet. Try clicking the buttons!"
            (str "Last action: " (name (:action (last history)))
                 " at " (.toLocaleTimeString (js/Date. (:timestamp (last history))))))]]]

       ;; Additional information card
       [card {:variant :subtle
              :class "max-w-md mx-auto mt-8"}
        [card-content {}
         [:div {:class "text-sm"}
          [:p {:class "mb-2"} "This counter demonstrates:"]
          [:ul {:class "list-disc pl-5 space-y-1"}
           [:li "State management with Reagent atoms"]
           [:li "Persistence with localStorage"]
           [:li "Conditional styling"]
           [:li "Action history tracking"]]]]]])))