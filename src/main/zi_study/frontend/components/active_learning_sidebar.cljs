(ns zi-study.frontend.components.active-learning-sidebar
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities :refer [cx]]
            ["lucide-react" :as lucide-icons]))

(defn sidebar-link [{:keys [href text icon active? on-click show-text?]}]
  [:a
   {:href href
    :on-click (fn [e]
                (when on-click (on-click e)))
    :title (when-not show-text? text)
    :class (cx "flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-all duration-200 group"
               "hover:bg-[var(--color-primary-alpha-10)] hover:text-[var(--color-primary)]"
               "dark:hover:bg-[var(--color-primary-alpha-20)] dark:hover:text-[var(--color-primary-300)]"
               (if active?
                 "bg-[var(--color-primary-alpha-15)] text-[var(--color-primary)] dark:bg-[var(--color-primary-alpha-20)] dark:text-[var(--color-primary-300)] shadow-sm"
                 "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"))}
   [:div {:class (cx "flex items-center justify-center w-8 h-8 rounded-md"
                     (if active?
                       "bg-[var(--color-primary-alpha-20)] dark:bg-[var(--color-primary-alpha-30)]"
                       ""))}
    [:> icon {:size 20
              :class "flex-shrink-0 group-hover:scale-110 transition-transform"}]]
   (when show-text?
     [:span {:class "ml-3 truncate"} text])])

(defn toggle-button [{:keys [is-open? on-click is-mobile?]}]
  [:button
   {:on-click on-click
    :title (if is-open? "Close Sidebar" "Open Sidebar")
    :class (cx "flex items-center justify-center shadow-md transition-all duration-300 ease-in-out"
               "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
               "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"
               "hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)]"

               (if is-open?
                 "absolute top-1/2 -right-3 transform -translate-y-1/2 z-50 h-10 w-6 rounded-r-md border-l-0"
                 "fixed left-0 top-1/2 -translate-y-1/2 z-50 h-12 w-12 rounded-r-lg border-l-0"))}
   [:> (if is-open?
         lucide-icons/ChevronLeft
         (if is-mobile? lucide-icons/Menu lucide-icons/PanelRight))
    {:size (if is-open? 18 20)
     :class "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"}]])

(defn active-learning-sidebar [{:keys [current-page links mobile-open? toggle-mobile-sidebar on-desktop-toggle]}]
  (r/with-let [;; Track desktop sidebar state
               desktop-open? (r/atom false)


               is-mobile (r/atom (.-matches (js/window.matchMedia "(max-width: 768px)")))


               mql (js/window.matchMedia "(max-width: 768px)")
               listener (fn [e] (reset! is-mobile (.-matches e)))


               _ (.addEventListener mql "change" listener)


               toggle-desktop (fn []
                                (swap! desktop-open? not)
                                (when on-desktop-toggle
                                  (on-desktop-toggle @desktop-open?)))]


    (r/create-class
     {:component-will-unmount
      (fn []
        (.removeEventListener mql "change" listener))

      :reagent-render
      (fn [{:keys [current-page links mobile-open? toggle-mobile-sidebar]}]
        (let [;; Calculate effective sidebar state based on device type
              effective-open? (if @is-mobile @mobile-open? @desktop-open?)]
          [:<>

           (when (and @is-mobile (not @mobile-open?))
             [toggle-button {:is-open? false
                             :on-click toggle-mobile-sidebar
                             :is-mobile? true}])


           (when (and (not @is-mobile) (not @desktop-open?))
             [toggle-button {:is-open? false
                             :on-click toggle-desktop
                             :is-mobile? false}])


           [:div {:class (cx "fixed inset-0 z-40 bg-black/30 transition-opacity duration-300"
                             (if effective-open?
                               "opacity-100 pointer-events-auto"
                               "opacity-0 pointer-events-none"))
                  :on-click (fn [e]
                              (.preventDefault e)
                              (.stopPropagation e)
                              (if @is-mobile
                                (toggle-mobile-sidebar)
                                (toggle-desktop)))}]


           [:aside
            {:class (cx "fixed top-0 left-0 z-50"
                        "h-screen w-64"
                        "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"
                        "border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                        "flex flex-col shadow-lg transition-transform duration-300 ease-in-out"
                        (if effective-open?
                          "transform-none"
                          "-translate-x-full"))}

            (when effective-open?
              [toggle-button {:is-open? true
                              :on-click (if @is-mobile toggle-mobile-sidebar toggle-desktop)
                              :is-mobile? false}])

            [:div {:class (cx "pt-12 px-4 pb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                              "flex items-center justify-between")}
             [:h2 {:class "text-lg font-semibold text-[var(--color-primary)] dark:text-[var(--color-primary-300)] truncate"}
              "Active Learning Tools"]]

            [:nav {:class "flex-grow p-4 space-y-2 overflow-y-auto scrollbar-thin scrollbar-thumb-[var(--color-light-divider)] dark:scrollbar-thumb-[var(--color-dark-divider)]"}
             (for [link links]
               ^{:key (:id link)}
               [sidebar-link {:href (:href link)
                              :text (:text link)
                              :icon (:icon link)
                              :active? (= (:id link) current-page)
                              :show-text? true
                              :on-click (fn [_]

                                          (when (and @is-mobile @mobile-open?)
                                            (toggle-mobile-sidebar)))}])]]]))}))) 