(ns zi-study.frontend.components.theme-switcher
  (:require [reagent.core :as r]
            [zi-study.frontend.utilities.theme :as theme]
            [zi-study.frontend.state :as state]
            ["lucide-react" :as lucide]))

(defn theme-switcher
  "A theme switcher component for light and dark themes"
  [{:keys [size class]
    :or {size :md}}]
  (let [btn-size-class (case size
                         :sm "h-8 w-8"
                         :md "h-10 w-10"
                         :lg "h-12 w-12"
                         "h-10 w-10")

        btn-class (str "flex items-center justify-center rounded-full "
                       "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] "
                       "hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)] "
                       "transition-colors duration-200 "
                       btn-size-class " "
                       class)]

    (fn []
      (let [current-theme (:theme (state/get-ui-state))]
        [:button {:class btn-class
                  :title (if (= current-theme :light)
                           "Switch to dark theme"
                           "Switch to light theme")
                  :on-click (fn [_] (theme/toggle-theme))}
         [:> (if (= current-theme :light)
               lucide/Sun
               lucide/Moon)
          {:size (case size
                   :sm 16
                   :md 20
                   :lg 24
                   20)}]]))))

(defn theme-selector
  "A dropdown selector for choosing between light and dark themes"
  []
  (let [open? (r/atom false)

        toggle-dropdown (fn [e]
                          (.preventDefault e)
                          (.stopPropagation e)
                          (swap! open? not))

        set-theme (fn [theme e]
                    (.preventDefault e)
                    (.stopPropagation e)
                    (theme/set-theme theme)
                    (reset! open? false))

        handle-click (fn [e]
                       (when (and @open?
                                  (not (.contains (.getElementById js/document "theme-selector") (.-target e))))
                         (reset! open? false)))

        setup-click-handler (fn []
                              (.addEventListener js/document "mousedown" handle-click)
                              #(.removeEventListener js/document "mousedown" handle-click))]

    (r/create-class
     {:component-did-mount
      (fn [_]
        (setup-click-handler))

      :component-will-unmount
      (fn [_]
        (.removeEventListener js/document "mousedown" handle-click))

      :reagent-render
      (fn [{:keys [class]}]
        (let [current-theme (:theme (state/get-ui-state))
              dropdown-class (str "absolute right-0 top-full mt-2 w-48 rounded-md shadow-lg "
                                  "bg-[var(--color-light-bg-elevated)] dark:bg-[var(--color-dark-bg-elevated)] "
                                  "ring-1 ring-black ring-opacity-5 "
                                  "divide-y divide-[var(--color-light-border)] dark:divide-[var(--color-dark-border)] "
                                  "focus:outline-none z-50 "
                                  (if @open? "block" "hidden"))]
          [:div {:id "theme-selector"
                 :class (str "relative " class)}

           [:button {:class (str "flex items-center space-x-2 px-3 py-2 rounded-md text-sm "
                                 "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] "
                                 "hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)] "
                                 "focus:outline-none")
                     :on-click toggle-dropdown}
            [:> (if (= current-theme :light)
                  lucide/Sun
                  lucide/Moon) {:size 16}]
            [:span {:class "ml-2"}
             (if (= current-theme :light)
               "Light"
               "Dark")]
            [:> lucide/ChevronDown {:size 16 :className "ml-2"}]]

           [:div {:class dropdown-class :role "menu"}
            [:div {:class "py-1" :role "none"}

             [:button {:class (str "flex items-center w-full px-4 py-2 text-sm "
                                   (if (= current-theme :light)
                                     "text-[var(--color-primary)] bg-[var(--color-light-bg-selected)] dark:bg-[var(--color-dark-bg-selected)]"
                                     "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]")
                                   " hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)]")
                       :role "menuitem"
                       :on-click #(set-theme :light %)}
              [:> lucide/Sun {:size 16 :className "mr-3"}]
              [:span "Light"]]

             [:button {:class (str "flex items-center w-full px-4 py-2 text-sm "
                                   (if (= current-theme :dark)
                                     "text-[var(--color-primary)] bg-[var(--color-light-bg-selected)] dark:bg-[var(--color-dark-bg-selected)]"
                                     "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]")
                                   " hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)]")
                       :role "menuitem"
                       :on-click #(set-theme :dark %)}
              [:> lucide/Moon {:size 16 :className "mr-3"}]
              [:span "Dark"]]]]]))})))