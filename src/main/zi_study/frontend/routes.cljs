(ns zi-study.frontend.routes
  (:require [zi-study.frontend.state :as state]
            [reitit.frontend.easy :as rfe]
            [zi-study.frontend.utilities.auth-guard :refer [auth-controller-start]]
            ["lucide-react" :as lucide-icons]))

(def sym-home-route ::home)
(def sym-active-learning-route ::active-learning)
(def sym-login-route ::login)
(def sym-register-route ::register)
(def sym-my-folders-route ::my-folders)
(def sym-folder-details-route ::folder-details)
(def sym-public-folders-route ::public-folders)
(def sym-advanced-search-route ::advanced-search)

(def sym-active-learning-question-sets-route ::active-learning-question-sets)
(def sym-set-page-route ::set-page)


(def common-auth-controllers
  [{:identity (fn [match] match)
    :start auth-controller-start}])

(def common-auth-opts
  {:redirect-to ::login
   :message "Please log in to access this page."})

(def topbar-nav-links
  [{:name sym-home-route :label "Home" :icon lucide-icons/Home}
   {:name sym-active-learning-route :label "Active Learning" :icon lucide-icons/Brain}])

(def active-learning-sub-route-names
  #{sym-active-learning-question-sets-route
    sym-set-page-route
    sym-advanced-search-route
    sym-my-folders-route
    sym-folder-details-route
    sym-public-folders-route})

(defn mk-routes
  [{:keys [main-layout active-learning-layout home-page login-page register-page question-sets-page set-page advanced-search-page my-folders-page folder-details-page public-folders-page]}]
  [["/"
    {:name sym-home-route
     :view home-page
     :layout main-layout}]

   ["/login"
    {:name sym-login-route
     :view login-page
     :layout main-layout}]

   ["/register"
    {:name sym-register-route
     :view register-page
     :layout main-layout}]

   ["/active-learning"
    {:layout active-learning-layout
     :controllers common-auth-controllers
     :view question-sets-page
     :protected? true
     :auth-opts common-auth-opts}
    [""
     {:name sym-active-learning-route
      :controllers [{:start (fn [_]
                              (js/setTimeout #(when (:authenticated? @(state/get-auth-state))
                                                (rfe/push-state ::active-learning-question-sets {})) 0))}]}]
    ["/question-sets"
     {:name sym-active-learning-question-sets-route
      :view question-sets-page
      :active-learning-page :question-sets}]
    ["/question-sets/:set-id"
     {:name sym-set-page-route
      :view set-page
      :auth-opts {:redirect-to ::login
                  :message "Please log in to view the question set"}
      :parameters {:path {:set-id int?}}}]
    ["/advanced-search"
     {:name sym-advanced-search-route
      :view advanced-search-page
      :active-learning-page :advanced-search}]
    ["/my-folders"
     {:name sym-my-folders-route
      :view my-folders-page
      :active-learning-page :my-folders}]
    ["/folders/:folder-id"
     {:name sym-folder-details-route
      :view folder-details-page
      :layout active-learning-layout
      :active-learning-page :folder-details
      :controllers common-auth-controllers
      :protected? true
      :auth-opts common-auth-opts
      :parameters {:path {:folder-id int?}}}]
    ["/public-folders"
     {:name sym-public-folders-route
      :view public-folders-page
      :layout active-learning-layout
      :active-learning-page :public-folders}]]])