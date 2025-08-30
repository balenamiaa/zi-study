(ns frontend.routes
  (:require ["lucide-react" :as lucide]))

(def sym-home-route ::home)
(def sym-todos-route ::todos)
(def sym-about-route ::about)

(def topbar-nav-links
  [{:name sym-home-route :label "Home" :icon lucide/Home}
   {:name sym-todos-route :label "Todos" :icon lucide/ListTodo}
   {:name sym-about-route :label "About" :icon lucide/Info}])

(defn mk-routes
  [{:keys [main-layout home-page todos-page about-page _not-found-page]}]
  [["/"
    {:name sym-home-route
     :view home-page
     :layout main-layout}]

   ["/todos"
    {:name sym-todos-route
     :view todos-page
     :layout main-layout}]

   ["/about"
    {:name sym-about-route
     :view about-page
     :layout main-layout}]])