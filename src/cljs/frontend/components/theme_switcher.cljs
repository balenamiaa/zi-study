(ns frontend.components.theme-switcher
  (:require ["lucide-react" :refer [Monitor Sun Moon]]
            [frontend.state.handlers :as state-h]
            [frontend.state.subs :as state-subs]
            [frontend.uix.hooks :as hooks]
            [re-frame.core :as rf]
            [uix.core :as uix :refer [$ defui]]))

(defui theme-switcher
  "A theme switcher component that allows selecting between system, light, and dark themes."
  []
  (let [ui (hooks/use-subscribe [::state-subs/ui-state])
        current-theme (:theme ui)
        themes [{:value :system :icon Monitor :title "Auto (System)"}
                {:value :light  :icon Sun :title "Light Theme"}
                {:value :dark   :icon Moon :title "Dark Theme"}]]

    ($ :div {:class "relative flex items-center p-0.5 rounded-full bg-base-200 border border-base-300"}

       (let [active-index (case current-theme
                            :system 0
                            :light  1
                            :dark   2
                            0)
             thumb-transform (str "translateX(" (* active-index 34) "px)")]
         ($ :div {:class "absolute top-[2px] left-[2px] h-8 w-8 rounded-full bg-primary shadow-md transition-all duration-300 ease-in-out"
                  :style {:transform thumb-transform}}))

       (for [{:keys [value icon title]} themes]
         ($ :button
            {:key value
             :title title
             :class "relative z-10 flex h-8 w-8 items-center justify-center rounded-full transition-colors duration-200 hover:text-primary-focus"
             :on-click #(rf/dispatch [::state-h/set-theme value])
             :aria-label title}
            ($ icon {:size 18
                     :className (if (= current-theme value)
                                  "text-primary-content"
                                  "text-base-content opacity-60 hover:opacity-100")}))))))
