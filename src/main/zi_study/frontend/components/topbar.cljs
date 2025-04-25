(ns zi-study.frontend.components.topbar
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [reitit.frontend.easy :as rfe]
   [zi-study.frontend.components.theme-switcher :refer [theme-switcher]]))


(defonce show-mobile-menu (r/atom false))
(defonce scroll-pos (r/atom 0))
(defonce shrink-nav (r/atom false))

;; Define navigation links internally
(def nav-links
  [{:name :zi-study.frontend.core/home :path "/" :label "Home" :icon lucide-icons/Home}
   {:name :zi-study.frontend.core/counter :path "/counter" :label "Counter" :icon lucide-icons/Hash}])

;; Animated logo component
(defn logo []
  [:div.flex.items-center.animate-pulse-slow
   [:div.mr-2.text-2xl.font-bold.text-transparent.bg-clip-text
    {:class "bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
    "ZiStudy"]
   [:> lucide-icons/Sparkles
    {:size 20
     :className "text-[var(--color-secondary)]"}]])

;; Mobile navigation menu
(defn mobile-menu [current-route]
  [:div
   {:class (if @show-mobile-menu
             "fixed inset-0 z-[100] bg-[var(--color-light-bg-paper)]/95 dark:bg-[rgba(38,26,37,0.98)] md:hidden"
             "hidden")}

   ;; Close button - Increase z-index
   [:div.absolute.top-4.right-4.z-10
    [:button.p-2.rounded-full
     {:class "hover:bg-[rgba(233,30,99,0.1)]"
      :on-click #(reset! show-mobile-menu false)
      :aria-label "Close menu"}
     [:> lucide-icons/X {:size 24 :className "text-[var(--color-primary)]"}]]]

   ;; Menu items
   [:nav.flex.flex-col.items-center.justify-center.h-full.text-xl.space-y-8.animate-slide-in
    (for [{:keys [name label icon]} nav-links]
      (let [is-active (= name current-route)]
        ^{:key (str name)}
        [:a.nav-link.py-2.px-4.w-48.text-center
         {:href (rfe/href name)
          :class (when is-active "active")
          :on-click #(reset! show-mobile-menu false)}
         [:div.flex.items-center.justify-center
          [:> icon {:size 20 :className "mr-2"}]
          label]]))]])

;; Main topbar component
(defn topbar [_]
  (let [handle-scroll (fn []
                        (let [current-pos (.. js/window -pageYOffset)]
                          (reset! shrink-nav (> current-pos 50))
                          (reset! scroll-pos current-pos)))]

    (r/create-class
     {:component-did-mount
      (fn []
        (js/window.addEventListener "scroll" handle-scroll))

      :component-will-unmount
      (fn []
        (js/window.removeEventListener "scroll" handle-scroll))

      :reagent-render
      (fn [{:keys [current-route]}]
        [:div
         ;; Mobile menu overlay
         [mobile-menu current-route]

         ;; Main navigation header
         [:header.sticky-header.animate-slide-down
          {:class (str "w-full transition-all duration-300 ease-in-out "
                       "bg-[var(--color-light-bg-paper)]/80 dark:bg-[var(--color-dark-bg-paper)]/90 "
                       (if @shrink-nav "py-2 shadow-md" "py-4"))}

          [:div.mx-auto.px-4.w-full
           [:div.flex.items-center.justify-between

            ;; Left section: Logo and desktop navigation
            [:div.flex.items-center
             ;; Logo
             [:a.mr-8 {:href "#"} [logo]]

             ;; Desktop navigation
             [:nav.hidden.md:flex.space-x-1
              (for [{:keys [name label icon]} nav-links]
                (let [is-active (= name current-route)]
                  ^{:key (str name)}
                  [:a.nav-link.py-2.px-3.rounded-md.flex.items-center
                   {:href (rfe/href name)
                    :class (when is-active "active")}
                   [:> icon {:size 18 :className "mr-1"}]
                   label]))]]

            ;; Right section: Actions
            [:div.flex.items-center.space-x-2

             ;; Search button with animation
             [:button.p-2.rounded-full.transition-all.duration-300.ease-in-out
              {:class (str "relative overflow-hidden hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)] "
                           (when @shrink-nav "scale-90"))
               :aria-label "Search"}
              [:> lucide-icons/Search
               {:size 20
                :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]"}]]

             ;; Notifications button with animation
             [:button.p-2.rounded-full.transition-all.duration-300.ease-in-out
              {:class (str "relative overflow-hidden hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)] "
                           (when @shrink-nav "scale-90"))
               :aria-label "Notifications"}
              [:> lucide-icons/Bell
               {:size 20
                :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]"}]]

             ;; Theme switcher component
             [:div {:class (str "transition-all duration-300 "
                                (when @shrink-nav "scale-90"))}
              [theme-switcher]]

             ;; User menu button
             [:button.ml-2.flex.items-center.justify-center.transition-all.duration-300.ease-in-out
              {:class (str "p-1 rounded-full overflow-hidden border-2 border-transparent focus:border-[var(--color-primary-300)] "
                           "hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)] "
                           (when @shrink-nav "scale-90"))
               :aria-label "User menu"}
              [:div.w-12.h-12.rounded-full.flex.items-center.justify-center.overflow-hidden.border-2
               {:class "bg-gradient-to-br from-[var(--color-primary-300)] to-[var(--color-primary-700)] border-[var(--color-primary-300)]"}
               [:img.w-full.h-full.rounded-full.object-cover
                {:src "https://images.ctfassets.net/h6goo9gw1hh6/2sNZtFAWOdP1lmQ33VwRN3/24e953b920a9cd0ff2e1d587742a2472/1-intro-photo-final.jpg?w=1200&h=992&fl=progressive&q=70&fm=jpg"}]]]

             ;; Mobile menu toggle
             [:button.ml-2.p-2.rounded-md.md:hidden
              {:class "hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)]"
               :on-click #(reset! show-mobile-menu true)
               :aria-label "Open menu"}
              [:> lucide-icons/Menu
               {:size 24
                :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]]]]]]])})))