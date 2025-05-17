(ns zi-study.frontend.components.folder-display-card
  (:require [reitit.frontend.easy :as rfe]
            ["lucide-react" :as lucide-icons]
            [zi-study.frontend.routes :as routes]
            [zi-study.frontend.components.card :refer [card card-content card-footer card-header]]
            [zi-study.frontend.components.spinner :refer [spinner]]
            [zi-study.frontend.utilities :refer [cx]]))

(defn folder-display-card [{:keys [folder on-click show-privacy-badge?]
                            :or {show-privacy-badge? true}}]
  (let [{:keys [folder-id name description is-public set-count updated-at email user-id creating?]} folder
        is-public (if (= is-public 1) true false)
        formatted-date (if updated-at
                         (.toLocaleDateString (js/Date. updated-at) "en-US"
                                              #js {:year "numeric" :month "short" :day "numeric"})
                         "...") ; Placeholder for temp card
        is-my-folder? (some? user-id)
        display-subtitle (if is-my-folder?
                           (str (or set-count 0) " " (if (= 1 set-count) "set" "sets"))
                           (when email (str "by " email)))]
    [card {:hover-effect (not creating?) ; Disable hover effect when creating
           :class (cx "h-full flex flex-col transition-all duration-200"
                      (if creating?
                        "opacity-60 cursor-default" ; Dim and disable cursor pointer
                        "cursor-pointer hover:shadow-lg dark:hover:shadow-[0_0_15px_rgba(255,255,255,0.1)]"))
           :on-click (when-not creating? ; Disable on-click when creating
                       (fn [_]
                         (if on-click
                           (on-click folder-id)
                           (rfe/push-state routes/sym-folder-details-route {:folder-id folder-id}))))}
     [card-header {:title name
                   :subtitle display-subtitle
                   :accent-color :primary
                   :action (when creating? [:div {:class "p-2"} [spinner {:size :sm}]])}] ; Show spinner in action slot
     [card-content {:class "flex-grow"}
      [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mb-3 line-clamp-3"}
       (or description (if creating? "Creating folder..." "No description provided."))]
      [:div {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] flex items-center gap-1 mb-1"}
       [:> lucide-icons/Clock {:size 14}]
       (str "Updated " formatted-date)]
      (when (and (not is-my-folder?) (some? set-count))
        [:div {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] flex items-center gap-1 mt-3"}
         [:> lucide-icons/Files {:size 14}]
         (str set-count " " (if (= 1 set-count) "set" "sets"))])
      (when (and is-my-folder? (not (some? set-count)))
        [:div {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] flex items-center gap-1 mt-3"}
         [:> lucide-icons/Files {:size 14}]
         "0 sets"])]
     [card-footer
      [:div {:class "flex items-center w-full gap-2"}
       (when show-privacy-badge?
         [:div {:class "flex items-center gap-1.5 text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
          [:> (if is-public lucide-icons/Globe lucide-icons/Lock) {:size 14}]
          [:span (if is-public "Public" "Private")]])
       [:div {:class "flex-grow"}]
       [:span {:class (cx "text-xs font-medium"
                          (if creating?
                            "text-[var(--color-light-text-disabled)] dark:text-[var(--color-dark-text-disabled)]"
                            "text-[var(--color-primary)] dark:text-[var(--color-primary-400)]"))}
        (if creating? "Creating..." "View Details")]]]]))