(ns frontend.components.theme-switcher
  (:require ["lucide-react" :refer [Monitor Sun Moon]]
            [frontend.state.ui :as state-ui]
            [frontend.state.router :as state-router]
            [frontend.uix.hooks :as hooks]
            [re-frame.core :as rf]
            [uix.core :as uix :refer [$ defui]]))

(defui theme-switcher
  "Unique, eye-candy theme switcher with animated thumb and gradient glow."
  []
  (let [ui (hooks/use-subscribe [::state-ui/ui-state])
        current-theme (:theme ui)
        options [{:value :system :icon Monitor :title "System"}
                 {:value :light  :icon Sun     :title "Light"}
                 {:value :dark   :icon Moon    :title "Dark"}]
        idx (case current-theme :system 0 :light 1 :dark 2 0)
        step 40 ;; px, matches h-10/w-10 = 2.5rem
        x (* idx step)]
    ($ :div {:class "relative inline-flex items-center px-1 py-1 rounded-full border border-base-300 bg-base-200/80 backdrop-blur-md shadow-md"}
       ;; Subtle ambient glow (lighter than previous conic)
       ($ :div {:class "pointer-events-none absolute -inset-1 rounded-full blur-lg opacity-30 glow-primary"})
       ;; Animated thumb (solid for stable contrast)
       ($ :div {:class "absolute top-1 left-1 h-10 w-10 rounded-full bg-primary shadow-lg transition-transform duration-300 ease-out"
                :style {:transform (str "translateX(" x "px)")}})
       ;; Buttons
       (for [{:keys [value icon title]} options]
         ($ :button {:key value
                     :title title
                     :on-click #(rf/dispatch [::state-ui/set-theme value])
                     :class "relative z-10 h-10 w-10 grid place-items-center rounded-full text-base-content/70 hover:text-base-content transition-colors"}
            ($ :div {:class "w-5 h-5 grid place-items-center"}
               ($ icon {:size 18
                        :className (when (= current-theme value) "text-primary-content")})))))))
