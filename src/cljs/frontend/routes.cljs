(ns frontend.routes
  (:require ["lucide-react" :refer [Home ListTodo Info]]))

(def sym-home-route ::home)
(def sym-todos-route ::todos)
(def sym-about-route ::about)
(def sym-profile-route ::profile)
(def sym-login-route ::login)
(def sym-register-route ::register)

(def topbar-nav-links
  [{:name sym-home-route :label "Home" :icon Home}
   {:name sym-todos-route :label "Todos" :icon ListTodo}
   {:name sym-about-route :label "About" :icon Info}])

(defn mk-routes
  [{:keys [main-layout home-page todos-page about-page profile-page login-page register-page _not-found-page]}]
  [["/"
    {:name sym-home-route
     :view home-page
     :layout main-layout}]

   ["/todos"
    {:name sym-todos-route
     :view todos-page
     :layout main-layout
     :protected? true}]

   ["/about"
    {:name sym-about-route
     :view about-page
     :layout main-layout}]

   ["/profile"
    {:name sym-profile-route
     :view profile-page
     :layout main-layout
     :protected? true}]

   ["/login"
    {:name sym-login-route
     :view login-page
     :layout main-layout}]

   ["/register"
    {:name sym-register-route
     :view register-page
     :layout main-layout}]])
