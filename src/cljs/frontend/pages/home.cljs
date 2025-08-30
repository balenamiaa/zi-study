(ns frontend.pages.home
  (:require ["lucide-react" :as lucide]
            [uix.core :as uix :refer [$ defui]]))

(defui home-page [_match]
  ($ :div {:class "min-h-[60vh] flex flex-col items-center justify-center"}
     ($ :div {:class "text-center space-y-6 max-w-3xl mx-auto px-4"}
        ($ :h1 {:class "text-6xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent animate-pulse"}
           "Welcome to Template")

        ($ :p {:class "text-xl text-base-content/80"}
           "A modern, beautiful ClojureScript application with UIx, DaisyUI, and Tailwind CSS")

        ($ :div {:class "flex flex-wrap gap-4 justify-center mt-8"}
           ($ :div {:class "stats shadow"}
              ($ :div {:class "stat"}
                 ($ :div {:class "stat-figure text-primary"}
                    ($ :> lucide/Zap {:size 32}))
                 ($ :div {:class "stat-title"} "Fast")
                 ($ :div {:class "stat-value text-primary"} "Lightning")
                 ($ :div {:class "stat-desc"} "Optimized performance"))

              ($ :div {:class "stat"}
                 ($ :div {:class "stat-figure text-secondary"}
                    ($ :> lucide/Palette {:size 32}))
                 ($ :div {:class "stat-title"} "Beautiful")
                 ($ :div {:class "stat-value text-secondary"} "Modern")
                 ($ :div {:class "stat-desc"} "Eye-candy UI"))

              ($ :div {:class "stat"}
                 ($ :div {:class "stat-figure text-accent"}
                    ($ :> lucide/Code2 {:size 32}))
                 ($ :div {:class "stat-title"} "Clean")
                 ($ :div {:class "stat-value text-accent"} "Code")
                 ($ :div {:class "stat-desc"} "Well structured"))))

        ($ :div {:class "flex gap-4 justify-center"}
           ($ :a {:href "/todos"
                  :class "btn btn-primary btn-lg gap-2"}
              ($ :> lucide/ArrowRight {:size 20})
              "Get Started")

           ($ :a {:href "/about"
                  :class "btn btn-outline btn-lg gap-2"}
              ($ :> lucide/Info {:size 20})
              "Learn More")))))