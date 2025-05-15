(ns zi-study.frontend.components.sidebar
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.utilities :refer [cx]]
            ["lucide-react" :as lucide-icons]
            [zi-study.frontend.components.button :refer [button]]))

(defn sidebar-link [{:keys [href text icon active? on-click collapsed?]}]
  [:a
   {:href href
    :on-click (fn [e]
                (when on-click (on-click e)))
    :title (when @collapsed? text) ;; Show full text on hover when collapsed
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
   (when-not @collapsed?
     [:span {:class "ml-3 truncate"} text])])

(defn sidebar-toggle-button [{:keys [on-click collapsed?]}]
  [:button
   {:on-click on-click
    :title (if @collapsed? "Expand Sidebar" "Collapse Sidebar")
    :class (cx
            (if @collapsed?
              ;; When collapsed - make the toggle button more prominent, but hide on mobile
              "fixed left-0 top-1/2 -translate-y-1/2 z-50 h-12 w-12 rounded-r-lg flex items-center justify-center hidden md:flex"
              ;; When expanded - attach to sidebar
              "absolute top-1/2 -right-3 transform -translate-y-1/2 z-50 h-10 w-6 rounded-r-md flex items-center justify-center")
            "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"
            "border border-l-0 border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
            "shadow-md hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)]"
            "transition-all duration-300 ease-in-out")}
   [:> (if @collapsed? lucide-icons/PanelRight lucide-icons/ChevronLeft)
    {:size (if @collapsed? 20 18)
     :class "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"}]])

(defn sidebar [{:keys [current-page links mobile-open? toggle-mobile-sidebar on-desktop-toggle]}]
  (r/with-let [collapsed? (r/atom true) ;; Persistent state atom for desktop collapse
               toggle-collapsed (fn []
                                 (swap! collapsed? not)
                                 (when on-desktop-toggle (on-desktop-toggle (not @collapsed?))))]
    [:<>
     ;; Backdrop for handling clicks outside sidebar
     [:div {:class (cx "fixed inset-0 z-40 bg-black/30 transition-opacity duration-300"
                       (if (or @mobile-open? (not @collapsed?))
                         "opacity-100 pointer-events-auto"
                         "opacity-0 pointer-events-none"))
            :on-click (fn [] 
                        (if (.-matches (js/window.matchMedia "(max-width: 768px)"))
                          (toggle-mobile-sidebar)
                          (toggle-collapsed)))}]

     ;; The toggle button (only shown when sidebar is collapsed on desktop)
     (when (and @collapsed? (not @mobile-open?))
       [sidebar-toggle-button {:on-click toggle-collapsed
                              :collapsed? collapsed?}])

     ;; Sidebar - positioned as overlay
     [:aside
      {:class (cx "fixed top-0 left-0 z-50" ; Increased z-index to overlay topbar
                  "h-screen w-64"
                  "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"
                  "border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                  "flex flex-col transition-all duration-300 ease-in-out"
                  "shadow-lg"
                  (if (or @mobile-open? (not @collapsed?))
                    "translate-x-0" 
                    "-translate-x-full"))}

      ;; Toggle button (only shown when sidebar is expanded)
      (when (not @collapsed?)
        [sidebar-toggle-button {:on-click toggle-collapsed
                                :collapsed? collapsed?}])

      ;; Sidebar Header - now starts at the top without any padding to overlay the topbar
      [:div {:class (cx "pt-16 px-4 pb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                        "flex items-center justify-between")}
       [:h2 {:class "text-lg font-semibold text-[var(--color-primary)] dark:text-[var(--color-primary-300)] truncate"}
        "Learning Tools"]]

      ;; Navigation Links
      [:nav {:class "flex-grow p-4 space-y-2 overflow-y-auto scrollbar-thin scrollbar-thumb-[var(--color-light-divider)] dark:scrollbar-thumb-[var(--color-dark-divider)]"}
       (for [link links]
         ^{:key (:id link)}
         [sidebar-link {:href (:href link)
                        :text (:text link)
                        :icon (:icon link)
                        :active? (= (:id link) current-page)
                        :collapsed? collapsed?
                        :on-click (fn [_]
                                    ;; Close mobile overlay sidebar if a link is clicked
                                    (when (and @mobile-open? toggle-mobile-sidebar)
                                      (toggle-mobile-sidebar)))}])]]])) 