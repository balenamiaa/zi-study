(ns frontend.pages.not-found
  (:require ["lucide-react" :refer [Home ArrowLeft]]
            [uix.core :as uix :refer [$ defui]]))

(defui not-found-page []
  ($ :div {:class "min-h-[60vh] flex items-center justify-center"}
     ($ :div {:class "text-center space-y-6"}
        ($ :div {:class "text-9xl font-bold text-primary/20"} "404")
        ($ :h1 {:class "text-4xl font-bold"} "Page Not Found")
        ($ :p {:class "text-xl text-base-content/70 max-w-md mx-auto"}
           "The page you're looking for doesn't exist or has been moved.")
        ($ :div {:class "flex gap-4 justify-center mt-8"}
           ($ :a {:href "/"
                  :class "btn btn-primary gap-2"}
              ($ Home {:size 20})
              "Go Home")
           ($ :button {:class "btn btn-outline gap-2"
                       :on-click #(.back js/window.history)}
              ($ ArrowLeft {:size 20})
              "Go Back")))))
