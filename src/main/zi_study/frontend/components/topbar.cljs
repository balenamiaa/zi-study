(ns zi-study.frontend.components.topbar
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [reitit.frontend.easy :as rfe]
   [zi-study.frontend.state :as state]
   [zi-study.frontend.utilities.auth-core :as auth-core]
   [zi-study.frontend.components.theme-switcher :refer [theme-switcher]]
   [zi-study.frontend.components.button :refer [button]]
   [zi-study.frontend.components.badge :refer [avatar]]
   [zi-study.frontend.components.dropdown :refer [dropdown menu-item menu-divider]]
   [zi-study.frontend.components.tooltip :refer [tooltip]]
   [zi-study.frontend.utilities :refer [cx]]
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
  [:div {:class "flex items-center"}
   [:div {:class (cx "mr-2 text-2xl font-bold text-transparent bg-clip-text"
                     "bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"
                     "hover:from-[var(--color-primary-500)] hover:to-[var(--color-primary-800)]"
                     "transition-all duration-300")}
    "ZiStudy"]
   [:> lucide-icons/Sparkles
    {:size 20
     :className "text-[var(--color-secondary)] animate-pulse transition-all"}]])

(defn action-button [{:keys [icon tooltip-text on-click aria-label shrink]}]
  [tooltip {:content tooltip-text
            :position :bottom
            :variant :light
            :trigger-class (when shrink "scale-90")}
   [button {:variant :text
            :size (if shrink :sm :md)
            :class (cx "transition-all duration-300")
            :on-click on-click
            :aria-label aria-label}
    [:> icon {:size (if shrink 18 20)
              :className (cx "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"
                             "hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]"
                             "transition-colors")}]]])

;; Mobile navigation menu
(defn mobile-menu [current-route]
  [:div
   {:class (cx "fixed inset-0 z-[100] transform transition-all duration-300"
               "bg-[var(--color-light-bg-paper)]/95 dark:bg-[rgba(38,26,37,0.98)] md:hidden"
               (if @show-mobile-menu
                 "translate-x-0 opacity-100"
                 "translate-x-full opacity-0 pointer-events-none"))}

   ;; Close button
   [:div {:class "absolute top-4 right-4 z-10"}
    [button {:variant :text
             :on-click #(reset! show-mobile-menu false)
             :aria-label "Close menu"
             :class "hover:rotate-90 transition-all duration-300"}
     [:> lucide-icons/X {:size 24 :className "text-[var(--color-primary)]"}]]]

   ;; Menu items
   [:nav {:class "flex flex-col items-center justify-center h-full text-xl space-y-6"}
    ;; User info at the top (if authenticated)
    (let [auth-state (state/get-auth-state)
          authenticated? (:authenticated? auth-state)
          current-user (:current-user auth-state)]
      (when (and authenticated? current-user)
        [:div {:class "flex flex-col items-center mb-6"}
         [avatar {:src (:profile_picture_url current-user)
                  :alt "User profile"
                  :size :lg
                  :initials (when (:email current-user)
                              (str/upper-case (subs (:email current-user) 0 1)))
                  :class "mb-2 border-2 border-[var(--color-primary-300)]"}]
         [:div {:class "text-center"}
          [:div {:class "font-medium text-base mb-0.5"}
           (:display_name current-user (:email current-user))]
          [:div {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
           (:email current-user)]]]))

    (for [{:keys [name label icon]} nav-links]
      (let [is-active (= name current-route)]
        ^{:key (str name)}
        [:a {:href (rfe/href name)
             :class (cx "nav-link py-3 px-6 w-48 text-center flex items-center justify-center space-x-3"
                        "transition-all duration-300 transform"
                        "hover:translate-y-[-2px]"
                        (when is-active "active"))
             :on-click #(reset! show-mobile-menu false)}
         [:> icon {:size 20 :className "flex-shrink-0"}]
         [:span label]]))

    ;; Render the same auth buttons that appear in the topbar
    [:div {:class "absolute bottom-8 left-0 right-0 flex justify-center space-x-4 mt-10"}
     (let [auth-state (state/get-auth-state)
           authenticated? (:authenticated? auth-state)]
       (if authenticated?
         [button {:variant :outlined
                  :size :md
                  :start-icon lucide-icons/LogOut
                  :on-click #(do
                               (auth-core/remove-token)
                               (state/reset-auth-state!)
                               (rfe/push-state :zi-study.frontend.core/home)
                               (reset! show-mobile-menu false))}
          "Sign Out"]
         [:<>
          [button {:variant :outlined
                   :size :md
                   :start-icon lucide-icons/LogIn
                   :on-click #(do
                                (rfe/push-state :zi-study.frontend.core/login)
                                (reset! show-mobile-menu false))}
           "Sign In"]
          [button {:variant :primary
                   :size :md
                   :start-icon lucide-icons/UserPlus
                   :on-click #(do
                                (rfe/push-state :zi-study.frontend.core/register)
                                (reset! show-mobile-menu false))}
           "Register"]]))]]])

;; User menu component (for authenticated users)
(defn user-menu [{:keys [user-info on-logout]}]
  (r/with-let [open? (r/atom false)
               force-update (r/atom 0)
               user-trigger [:div {:class "flex items-center cursor-pointer hover:opacity-80 transition-opacity duration-200"}
                             [avatar {:src (:profile_picture_url user-info)
                                      :alt "User profile"
                                      :size :sm
                                      :initials (when (:email user-info)
                                                  (str/upper-case (subs (:email user-info) 0 1)))
                                      :class "mr-2 border-2 border-[var(--color-light-bg-paper)] dark:border-[var(--color-dark-bg-paper)]"}]
                             [:span
                              {:class "hidden sm:inline text-sm font-medium text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] max-w-[120px] truncate"}
                              (:display_name user-info (:email user-info))
                              [:> lucide-icons/ChevronDown
                               {:size 16 :className "ml-1 inline-block text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]]]]
    [dropdown {:trigger user-trigger
               :placement :bottom-right
               :width "w-48"
               :transition :scale
               :open? open?
               :on-open #(swap! force-update inc)
               :class "p-1.5 sm:p-2"
               :trigger-class "relative"}

     ;; User info section at top
     [:div {:class "py-1.5 sm:py-2 px-2 sm:px-3 mb-1.5 sm:mb-2 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
      [:div {:class "font-medium text-sm mb-0.5 truncate"}
       (:display_name user-info (:email user-info))]
      [:div {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] truncate"}
       (:email user-info)]]

     ;; Menu items
     [menu-item {:icon lucide-icons/User
                 :on-click #(js/console.log "Profile clicked")
                 :class "py-1.5 sm:py-2 text-sm"}
      "Profile"]

     [menu-item {:icon lucide-icons/Settings
                 :on-click #(js/console.log "Settings clicked")
                 :class "py-1.5 sm:py-2 text-sm"}
      "Settings"]

     [menu-divider]

     [menu-item {:icon lucide-icons/LogOut
                 :danger? true
                 :on-click on-logout
                 :class "py-1.5 sm:py-2 text-sm"}
      "Logout"]]))


;; Main topbar component
(defn topbar []
  (let [handle-scroll (fn []
                        (let [current-pos (.. js/window -pageYOffset)]
                          (reset! shrink-nav (> current-pos 50))
                          (reset! scroll-pos current-pos)))

        handle-logout (fn []
                        (auth-core/remove-token)
                        (state/reset-auth-state!)
                        (rfe/push-state :zi-study.frontend.core/home))]

    (r/create-class
     {:component-did-mount
      (fn []
        (js/window.addEventListener "scroll" handle-scroll))

      :component-will-unmount
      (fn []
        (js/window.removeEventListener "scroll" handle-scroll))

      :reagent-render
      (fn [{:keys [current-route]}]
        (let [auth-state (state/get-auth-state)
              authenticated? (:authenticated? auth-state)
              current-user (:current-user auth-state)
              auth-loading? (:loading? auth-state)]
          [:div
           ;; Mobile menu overlay
           [mobile-menu current-route]
           ;; Main navigation header
           [:header {:class (cx "sticky-header w-full transition-all duration-300 ease-in-out z-50"
                                "bg-[var(--color-light-bg-paper)]/90 dark:bg-[var(--color-dark-bg-paper)]/90"
                                "border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                                (if @shrink-nav "py-1 shadow-md" "py-3"))}

            [:div {:class "mx-auto px-4 w-full max-w-7xl"}
             [:div {:class "flex items-center justify-between"}

              ;; Left section: Logo and desktop navigation
              [:div {:class "flex items-center"}
               ;; Logo
               [:a {:href (rfe/href :zi-study.frontend.core/home)
                    :class (cx "mr-8 transition-transform duration-300"
                               (when @shrink-nav "scale-95"))}
                [logo]]

               ;; Desktop navigation
               [:nav {:class "hidden md:flex space-x-1"}
                (for [{:keys [name label icon]} nav-links]
                  (let [is-active (= name current-route)]
                    ^{:key (str name)}
                    [:a {:href (rfe/href name)
                         :class (cx "nav-link py-2 px-3 rounded-md flex items-center transition-all duration-200"
                                    "hover:translate-y-[-2px]"
                                    (when is-active "active"))}
                     [:> icon {:size 18 :className "mr-1.5 flex-shrink-0"}]
                     label]))]]

              ;; Right section: Actions
              [:div {:class "flex items-center space-x-2"}

               ;; Search button - visible on all screens
               [action-button
                {:icon lucide-icons/Search
                 :tooltip-text "Search"
                 :aria-label "Search"
                 :shrink @shrink-nav}]

               ;; Theme switcher component - visible on all screens
               [:div {:class (cx "transition-all duration-300"
                                 (when @shrink-nav "scale-90"))}
                [theme-switcher]]

               ;; Authentication-based UI
               (cond
                 auth-loading?
                 ;; Show loading indicator
                 [:div {:class "w-8 h-8 flex items-center justify-center"}
                  [:div {:class "animate-spin rounded-full h-5 w-5 border-b-2 border-t-2 border-[var(--color-primary)]"}]]

                 authenticated?
                 ;; User is logged in - show user menu
                 (when current-user
                   [:div {:class (cx "transition-all duration-300"
                                     (when @shrink-nav "scale-95"))}
                    [user-menu {:user-info current-user
                                :on-logout handle-logout}]])

                 :else
                 ;; User is not logged in - show login/register buttons
                 [:div {:class (cx "flex flex-shrink-0 items-center space-x-2 transition-all duration-300"
                                   (when @shrink-nav "scale-95"))}
                  [:div {:class "hidden sm:block"}
                   [button {:variant :outlined
                            :size :sm
                            :start-icon lucide-icons/LogIn
                            :on-click #(rfe/push-state :zi-study.frontend.core/login)}
                    "Sign in"]]

                  [button {:variant :primary
                           :size :sm
                           :start-icon lucide-icons/UserPlus
                           :class "hidden sm:block shadow-md hover:shadow-lg transition-shadow"
                           :on-click #(rfe/push-state :zi-study.frontend.core/register)}
                   "Register"]])

               ;; Mobile menu toggle
               [button {:variant :text
                        :class "ml-1 md:hidden"
                        :size (if @shrink-nav :sm :md)
                        :on-click #(reset! show-mobile-menu true)
                        :aria-label "Open menu"
                        :aria-expanded @show-mobile-menu}
                [:> lucide-icons/Menu
                 {:size (if @shrink-nav 20 24)
                  :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]]]]]]]))})))