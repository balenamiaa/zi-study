(ns zi-study.frontend.components.theme-switcher
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.theme :as theme]
            [zi-study.frontend.state :as state]
            ["lucide-react" :as lucide]
            [zi-study.frontend.utilities :refer [cx]]))

(defn theme-switcher
  "A theme switcher component that allows selecting between system, light, and dark themes.
  The UI is a pill-shaped control with a sliding indicator for the active theme."
  []
  (let [current-theme-atom (r/reaction (:theme (state/get-ui-state)))
        themes [{:value :system :icon lucide/Monitor :title "Auto (System Preference)"}
                {:value :light  :icon lucide/Sun :title "Light Theme"}
                {:value :dark   :icon lucide/Moon :title "Dark Theme"}]]

    (fn []
      [:div {:class (cx "relative flex items-center space-x-0.5 p-0.5 rounded-full"
                        "bg-[var(--color-light-bg-hover)] dark:bg-[var(--color-dark-bg-paper)]"
                        "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                        "shadow-sm ")
             :role "radiogroup"
             :aria-label "Theme selection"}

       ;; Sliding thumb that highlights the active selection
       (let [active-index (case @current-theme-atom
                            :system 0
                            :light  1
                            :dark   2
                            0) ; Default to system index
             ;; Buttons are h-8 w-8 (32px). space-x-0.5 (2px) between them.
             ;; Container p-0.5 (2px padding). Thumb positioned with top-[2px] left-[2px].
             ;; Movement unit for translateX = button_width (32px) + space_between_buttons (2px) = 34px.
             thumb-transform (str "translateX(" (* active-index 34) "px)")]
         [:div {:class (cx "absolute top-[2px] left-[2px] h-8 w-8 rounded-full"
                           "bg-white dark:bg-[var(--color-dark-card)]" ; Thumb background
                           "shadow-md transition-all duration-300 ease-in-out")
                :style {:transform thumb-transform}}])

       ;; Buttons for each theme option
       (for [{:keys [value icon title]} themes]
         ^{:key (keyword (str "theme-btn-" (name value)))}
         [:button
          {:title title
           :class (cx "relative z-10 flex h-8 w-8 items-center justify-center rounded-full"
                      "focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)] focus-visible:ring-offset-2"
                      "focus-visible:ring-offset-[var(--color-light-bg)] dark:focus-visible:ring-offset-[var(--color-dark-bg)]"
                      "transition-colors duration-100")
           :on-click #(theme/set-theme value)
           :aria-label title
           :role "radio"
           :aria-checked (= @current-theme-atom value)}
          [:> icon {:size 18 ; Slightly smaller icon for better padding within the 32px button
                    :class (cx "transition-colors duration-100"
                               (if (= @current-theme-atom value)
                                 ;; Active icon color (contrasts with thumb)
                                 "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"
                                 ;; Inactive icon color
                                 "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]")
                               ;; Hover color for inactive icons (only apply if not active)
                               (when (not= @current-theme-atom value)
                                 "hover:text-[var(--color-light-text-primary)] dark:hover:text-[var(--color-dark-text-primary)]"))}]])])))