(ns frontend.layouts.main-layout
  (:require ["lucide-react" :refer [Menu Zap User UserCircle Settings LogOut Heart Image]]
            [frontend.components.theme-switcher :refer [theme-switcher]]
            [frontend.routes :as routes :refer [topbar-nav-links]]
            [frontend.state.auth :as auth]
            [frontend.state.user :as user]
            [frontend.ui.avatar :refer [Avatar]]
            [frontend.ui.dropdown :refer [Dropdown]]
            [frontend.ui.feedback :refer [Toasts]]
            [frontend.uix.hooks :refer [use-subscribe]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(defui nav-link [{:keys [name label icon active?]}]
  ($ :a
     {:href (rfe/href name)
      :on-click (fn [e]
                  (.preventDefault e)
                  (rfe/push-state name))
      :class (str "btn btn-ghost rounded-lg px-4 py-2 transition-all duration-200 "
                  (if active?
                    "bg-primary/10 text-primary hover:bg-primary/20"
                    "hover:bg-base-200"))}
     (when icon
       ($ icon {:size 20 :className "mr-2"}))
     ($ :span label)))

(defui header [{:keys [current-route]}]
  ($ :header
     {:class "navbar bg-base-100 border-b border-base-200 px-4 lg:px-8"}

     ($ :div {:class "navbar-start"}
        ($ :div {:class "dropdown lg:hidden"}
           ($ :button
              {:tabIndex 0
               :class "btn btn-ghost btn-circle"
               :aria-label "Open menu"}
              ($ Menu {:size 24}))
           ($ :ul
              {:tabIndex 0
               :class "menu menu-sm dropdown-content mt-3 z-[1] p-2 shadow-lg bg-base-100 rounded-box w-52 border border-base-200"}
              (for [{:keys [name _label] :as link} topbar-nav-links]
                ($ :li {:key name}
                   ($ nav-link (assoc link :active? (= current-route name)))))))

        ($ :div {:class "flex items-center space-x-2"}
           ($ Zap {:size 28 :className "text-primary"})
           ($ :span {:class "text-xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent"}
              "Template")))

     ($ :div {:class "navbar-center hidden lg:flex"}
        ($ :ul {:class "menu menu-horizontal px-1 gap-2"}
           (for [{:keys [name] :as link} topbar-nav-links]
             ($ :li {:key name}
                ($ nav-link (assoc link :active? (= current-route name)))))))

     ($ :div {:class "navbar-end gap-3"}
        ($ theme-switcher)

        (let [auth (use-subscribe [::auth/auth])
              status (:status auth)
              raw-user (:user auth)
              user (cond
                     (and (map? raw-user) (:body raw-user)) (:body raw-user)
                     (map? raw-user) raw-user
                     :else nil)
              avatar (:avatar-url user)]
          (cond
            (= status :loading)
            ($ :div {:class "btn btn-ghost btn-circle"}
               ($ :span {:class "loading loading-spinner text-primary"}))

            (= status :authenticated)
            ($ Dropdown {:align :end
                         :trigger ($ :div {:class "btn btn-ghost btn-circle"}
                                     ($ Avatar {:src avatar :name (:name user) :email (:email user) :size :md}))
                         :children ($ :ul nil
                                      ($ :li ($ :a {:href "#"
                                                    :on-click (fn [e] (.preventDefault e)
                                                                (rfe/push-state routes/sym-profile-route))}
                                                ($ UserCircle {:size 16 :className "mr-2"})
                                                "Profile"))
                                      ($ :li ($ :a {:href "#" :on-click #(rf/dispatch [::auth/logout])}
                                                ($ LogOut {:size 16 :className "mr-2"})
                                                "Logout")))})

            :else
            ($ :div {:class "flex gap-2"}
               ($ :button {:class "btn btn-sm btn-primary"
                           :on-click #(rfe/push-state routes/sym-login-route)} "Login")
               ($ :button {:class "btn btn-sm btn-outline"
                           :on-click #(rfe/push-state (keyword (namespace routes/sym-login-route) "register"))} "Register")))))))

(defui footer []
  ($ :footer {:class "footer footer-center p-10 bg-base-200 text-base-content rounded-t-3xl mt-auto"}
     ($ :nav {:class "flex flex-wrap gap-4"}
        ($ :a {:href "#" :class "link link-hover"} "About")
        ($ :a {:href "#" :class "link link-hover"} "Contact")
        ($ :a {:href "#" :class "link link-hover"} "Privacy")
        ($ :a {:href "#" :class "link link-hover"} "Terms"))
     ($ :aside
        ($ :p {:class "flex items-center gap-2"}
           "© 2025 Template. Built with"
           ($ Heart {:size 16 :className "text-error fill-error"})
           "and ClojureScript"))))

(defui main-layout [{:keys [current-route children]}]
  ($ :div {:class "min-h-screen flex flex-col bg-base-100"}
     ($ header {:current-route current-route})
     ($ :main {:class "flex-1 container mx-auto px-4 py-8 lg:px-8"}
        children)
     ($ footer)
     ($ Toasts {:portal? true :position :top-right})))
