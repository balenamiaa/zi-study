(ns frontend.pages.about
  (:require ["lucide-react" :as lucide]
            [uix.core :as uix :refer [$ defui]]))

(defui feature-card [{:keys [icon title description]}]
  ($ :div {:class "card bg-base-200 shadow-xl hover:shadow-2xl transition-all hover:-translate-y-1"}
     ($ :div {:class "card-body"}
        ($ :div {:class "flex items-center gap-3 mb-3"}
           ($ :div {:class "p-3 rounded-lg bg-primary/10"}
              ($ :> icon {:size 24 :class "text-primary"}))
           ($ :h3 {:class "card-title"} title))
        ($ :p {:class "text-base-content/80"} description))))

(defui about-page [_match]
  ($ :div {:class "max-w-6xl mx-auto space-y-12"}
     ($ :div {:class "hero bg-gradient-to-br from-primary/10 to-secondary/10 rounded-3xl"}
        ($ :div {:class "hero-content text-center py-12"}
           ($ :div {:class "max-w-md"}
              ($ :h1 {:class "text-5xl font-bold mb-4"} "About Template")
              ($ :p {:class "text-lg"}
                 "A modern ClojureScript application showcasing the best of functional programming and beautiful UI design"))))

     ($ :section {:class "space-y-6"}
        ($ :h2 {:class "text-3xl font-bold text-center"} "Technologies")
        ($ :div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"}
           ($ feature-card
              {:icon lucide/Package
               :title "UIx"
               :description "React wrapper for ClojureScript with excellent performance and developer experience"})

           ($ feature-card
              {:icon lucide/Palette
               :title "DaisyUI"
               :description "Beautiful component library built on top of Tailwind CSS with semantic color system"})

           ($ feature-card
              {:icon lucide/Wind
               :title "Tailwind CSS"
               :description "Utility-first CSS framework for rapidly building custom user interfaces"})

           ($ feature-card
              {:icon lucide/Database
               :title "Re-frame"
               :description "A pattern for writing SPAs in ClojureScript, using Reagent's reactive data flow"})

           ($ feature-card
              {:icon lucide/Route
               :title "Reitit"
               :description "Fast data-driven router for Clojure(Script) with coercion and middleware support"})

           ($ feature-card
              {:icon lucide/Sparkles
               :title "Lucide Icons"
               :description "Beautiful and consistent icon set with over 1000+ icons for modern web apps"})))

     ($ :section {:class "space-y-6"}
        ($ :h2 {:class "text-3xl font-bold text-center"} "Features")
        ($ :div {:class "space-y-4"}
           ($ :div {:class "alert alert-info"}
              ($ :> lucide/Moon {:size 20})
              ($ :span "Dark mode support with system preference detection"))

           ($ :div {:class "alert alert-success"}
              ($ :> lucide/Smartphone {:size 20})
              ($ :span "Fully responsive design that works on all devices"))

           ($ :div {:class "alert alert-warning"}
              ($ :> lucide/Zap {:size 20})
              ($ :span "Lightning fast performance with optimized builds"))

           ($ :div {:class "alert"}
              ($ :> lucide/Code2 {:size 20})
              ($ :span "Clean, maintainable code structure with functional programming"))))))