(ns zi-study.frontend.pages.not-found
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [reitit.frontend.easy :as rfe]
   [zi-study.frontend.components.button :refer [button]]))

(defn not-found-page []
  (r/create-class
   {:component-did-mount
    (fn []
      ;; Add any initialization logic here if needed
      )

    :reagent-render
    (fn []
      [:div
       {:class "flex flex-col items-center justify-center min-h-[calc(100vh-200px)] p-4"}

       ;; Animated 404 heading
       [:h1
        {:class "text-9xl font-bold mb-4 animate-pulse-slow text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"}
        "404"]

       ;; Page title with gradient text
       [:h2
        {:class "text-3xl font-semibold mb-6 text-center text-transparent bg-clip-text bg-gradient-to-r from-[var(--color-primary-400)] to-[var(--color-primary-700)]"}
        "Page Not Found"]

       ;; Description with illustration
       [:div
        {:class "flex flex-col md:flex-row items-center max-w-3xl mb-8 animate-slide-in"}

        ;; Left side: Icon and message
        [:div
         {:class "text-center md:text-left md:mr-8 mb-6 md:mb-0"}
         [:div
          {:class "flex justify-center md:justify-start mb-4"}
          [:> lucide-icons/Map {:size 64, :className "text-[var(--color-secondary)] dark:text-[var(--color-secondary-light)] opacity-80"}]]

         [:p
          {:class "text-lg mb-4 text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
          "Looks like you've taken a wrong turn."]

         [:p
          {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
          "The page you're looking for doesn't exist or has been moved to another location."]]

        ;; Right side: Lost illustration
        [:div
         {:class "relative w-full md:w-64 h-64"}
         [:div
          {:class "absolute inset-0 flex items-center justify-center"}
          [:> lucide-icons/Compass {:size 48, :className "text-[var(--color-primary-200)] dark:text-[var(--color-primary-700)] absolute animate-pulse"}]
          [:> lucide-icons/CircleDashed {:size 120, :className "text-[var(--color-primary-100)] dark:text-[var(--color-primary-800)] absolute animate-spin-slow opacity-50"}]]

         ;; Small decorative elements
         [:div
          {:class "absolute top-1/4 left-1/4 transform -translate-x-1/2 -translate-y-1/2"}
          [:> lucide-icons/MapPin {:size 16, :className "text-[var(--color-error-400)] animate-bounce"}]]

         [:div
          {:class "absolute bottom-1/4 right-1/4 transform translate-x-1/2 translate-y-1/2"}
          [:> lucide-icons/Star {:size 16, :className "text-[var(--color-warning-400)] animate-pulse"}]]

         [:div
          {:class "absolute top-1/2 right-1/3 transform translate-x-1/2 -translate-y-1/2"}
          [:> lucide-icons/CircleDot {:size 12, :className "text-[var(--color-info-400)] animate-ping"}]]]]

       ;; Action buttons
       [:div
        {:class "flex flex-wrap gap-4 justify-center"}
        [button {:variant :primary
                 :start-icon lucide-icons/Home
                 :on-click #(rfe/push-state :zi-study.frontend.core/home)}
         "Go Home"]

        [button {:variant :outlined
                 :start-icon lucide-icons/ArrowLeft
                 :on-click #(js/window.history.back)}
         "Go Back"]

        [button {:variant :text
                 :start-icon lucide-icons/Search
                 :on-click #(js/console.log "Search clicked")}
         "Search"]]])}))

;; Add keyframes for the custom spin animation
(defn add-keyframes []
  (let [style-el (.createElement js/document "style")]
    (set! (.-innerHTML style-el)
          "@keyframes spin-slow { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
           .animate-spin-slow { animation: spin-slow 6s linear infinite; }
           @keyframes ping { 75%, 100% { transform: scale(2); opacity: 0; } }
           .animate-ping { animation: ping 2s cubic-bezier(0, 0, 0.2, 1) infinite; }")
    (.appendChild (.-head js/document) style-el)))

;; Execute the function to add keyframes
(add-keyframes)