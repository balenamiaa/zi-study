(ns zi-study.frontend.components.topbar
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [reitit.frontend.easy :as rfe]
   [zi-study.frontend.state :as state]
   [zi-study.frontend.components.theme-switcher :refer [theme-switcher]]
   [zi-study.frontend.components.button :refer [button]]
   [zi-study.frontend.components.badge :refer [avatar]]
   [zi-study.frontend.components.dropdown :refer [dropdown menu-item menu-divider]]
   [clojure.string :as str]))


(defonce show-mobile-menu (r/atom false))
(defonce scroll-pos (r/atom 0))
(defonce shrink-nav (r/atom false))

;; Define navigation links internally
(def nav-links
  [{:name :zi-study.frontend.core/home :path "/" :label "Home" :icon lucide-icons/Home}
   {:name :zi-study.frontend.core/counter :path "/counter" :label "Counter" :icon lucide-icons/Hash}
   {:name :zi-study.frontend.core/components :path "/components" :label "Components" :icon lucide-icons/Layers}])

;; Animated logo component
(defn logo []
  [:div {:class "flex items-center animate-pulse-slow"}
   [:div {:class "mr-2 text-2xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
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

   ;; Close button
   [:div {:class "absolute top-4 right-4 z-10"}
    [button {:variant :text
             :on-click #(reset! show-mobile-menu false)
             :aria-label "Close menu"}
     [:> lucide-icons/X {:size 24 :className "text-[var(--color-primary)]"}]]]

   ;; Menu items
   [:nav {:class "flex flex-col items-center justify-center h-full text-xl space-y-8 animate-slide-in"}
    (for [{:keys [name label icon]} nav-links]
      (let [is-active (= name current-route)]
        ^{:key (str name)}
        [:a {:href (rfe/href name)
             :class (str "nav-link py-2 px-4 w-48 text-center " (when is-active "active"))
             :on-click #(reset! show-mobile-menu false)}
         [:div {:class "flex items-center justify-center"}
          [:> icon {:size 20 :className "mr-2"}]
          label]]))]])

;; User menu component (for authenticated users)
(defn user-menu [{:keys [user-info on-logout]}]
  (let [user-trigger [:div {:class "flex items-center cursor-pointer"}
                      [avatar {:src (:profile_picture_url user-info)
                               :alt "User profile"
                               :size :sm
                               :initials (when (:email user-info)
                                           (str/upper-case (subs (:email user-info) 0 1)))
                               :class "mr-2"}]
                      [:span
                       {:class "text-sm font-medium text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] max-w-[120px] truncate"}
                       (:email user-info)]]]

    [dropdown {:trigger user-trigger
               :placement :bottom-right
               :width "w-48"
               :transition :scale}

     [menu-item {:icon lucide-icons/User
                 :on-click #(js/console.log "Profile clicked")}
      "Profile"]

     [menu-item {:icon lucide-icons/Settings
                 :on-click #(js/console.log "Settings clicked")}
      "Settings"]

     [menu-item {:icon lucide-icons/HelpCircle
                 :on-click #(js/console.log "Help clicked")}
      "Help"]

     [menu-divider]

     [menu-item {:icon lucide-icons/LogOut
                 :danger true
                 :on-click on-logout}
      "Logout"]]))

;; Main topbar component
(defn topbar []
  (let [handle-scroll (fn []
                        (let [current-pos (.. js/window -pageYOffset)]
                          (reset! shrink-nav (> current-pos 50))
                          (reset! scroll-pos current-pos)))

        handle-logout (fn []
                        (.removeItem js/localStorage "auth-token")
                        (swap! state/app-state assoc
                               :auth/authenticated? false
                               :auth/token nil
                               :auth/current-user nil
                               :auth/loading? false)
                        (rfe/push-state :zi-study.frontend.core/home))]

    (r/create-class
     {:component-did-mount
      (fn []
        (js/window.addEventListener "scroll" handle-scroll))

      :component-will-unmount
      (fn []
        (js/window.removeEventListener "scroll" handle-scroll))

      :reagent-render
      (fn [{:keys [current-route auth-state]}]
        (let [authenticated? (:auth/authenticated? auth-state)
              current-user (:auth/current-user auth-state)
              auth-loading? (:auth/loading? auth-state)]
          [:div
           ;; Mobile menu overlay
           [mobile-menu current-route]

           ;; Main navigation header
           [:header {:class (str "sticky-header animate-slide-down w-full transition-all duration-300 ease-in-out "
                                 "bg-[var(--color-light-bg-paper)]/80 dark:bg-[var(--color-dark-bg-paper)]/90 "
                                 (if @shrink-nav "py-2 shadow-md" "py-4"))}

            [:div {:class "mx-auto px-4 w-full"}
             [:div {:class "flex items-center justify-between"}

              ;; Left section: Logo and desktop navigation
              [:div {:class "flex items-center"}
               ;; Logo
               [:a {:href (rfe/href :zi-study.frontend.core/home)
                    :class "mr-8"}
                [logo]]

               ;; Desktop navigation
               [:nav {:class "hidden md:flex space-x-1"}
                (for [{:keys [name label icon]} nav-links]
                  (let [is-active (= name current-route)]
                    ^{:key (str name)}
                    [:a {:href (rfe/href name)
                         :class (str "nav-link py-2 px-3 rounded-md flex items-center " (when is-active "active"))}
                     [:> icon {:size 18 :className "mr-1"}]
                     label]))]]

              ;; Right section: Actions
              [:div {:class "flex items-center space-x-2"}

               ;; Search button
               [button {:variant :text
                        :class (when @shrink-nav "scale-90")
                        :aria-label "Search"}
                [:> lucide-icons/Search
                 {:size 20
                  :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]"}]]

               ;; Theme switcher component
               [:div {:class (str "transition-all duration-300 "
                                  (when @shrink-nav "scale-90"))}
                [theme-switcher]]

               ;; Authentication-based UI
               (cond
                 auth-loading?
                 ;; Show loading indicator
                 [:div {:class "w-8 h-8 flex items-center justify-center"}
                  [:div {:class "animate-spin rounded-full h-5 w-5 border-b-2 border-[var(--color-primary)]"}]]

                 authenticated?
                 ;; User is logged in - show user menu
                 (when current-user
                   [user-menu {:user-info current-user
                               :on-logout handle-logout}])

                 :else
                 ;; User is not logged in - show login/register buttons
                 [:div {:class "flex items-center space-x-2"}
                  [button {:variant :outlined
                           :size :sm
                           :start-icon lucide-icons/LogIn
                           :on-click #(rfe/push-state :zi-study.frontend.core/login)}
                   "Sign in"]

                  [button {:variant :primary
                           :size :sm
                           :start-icon lucide-icons/UserPlus
                           :on-click #(rfe/push-state :zi-study.frontend.core/register)}
                   "Register"]])

               ;; Mobile menu toggle
               [button {:variant :text
                        :class "ml-2 md:hidden"
                        :on-click #(reset! show-mobile-menu true)
                        :aria-label "Open menu"}
                [:> lucide-icons/Menu
                 {:size 24
                  :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]]]]]]]))})))