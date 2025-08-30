(ns frontend.components.theme-switcher
  (:require [uix.core :as uix :refer [$ defui]]
            ["lucide-react" :as lucide]
            [frontend.utilities.theme :as theme]
            [frontend.state :as state]))

(defui theme-switcher
  "A theme switcher component that allows selecting between system, light, and dark themes."
  []
  (let [current-theme (uix/use-subscribe #(:theme @(state/get-ui-state)))
        themes [{:value :system :icon lucide/Monitor :title "Auto (System)"}
                {:value :light  :icon lucide/Sun :title "Light Theme"}
                {:value :dark   :icon lucide/Moon :title "Dark Theme"}]]

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
             :on-click #(theme/set-theme value)
             :aria-label title}
            ($ :> icon {:size 18
                        :class (if (= current-theme value)
                                 "text-primary-content"
                                 "text-base-content opacity-60 hover:opacity-100")}))))))