(ns zi-study.frontend.components.popover
  (:require
   [reagent.core :as r]))

(defn popover
  "A popover component that displays floating content relative to a trigger element.
  
   Options:
   - trigger: element that triggers the popover (required)
   - is-open: boolean to control visibility (use with on-change for controlled usage)
   - on-change: function to call when open state changes (fn [new-state])
   - placement: :top, :right, :bottom, :left (default :bottom)
   - arrow: true/false - show directional arrow (default true)
   - class: additional CSS classes for the popover
   - close-on-click-outside: true/false (default true)
   - content: either a React element or function that receives close fn
   - transition: :fade, :scale, :slide (default :scale)"
  []

  (let [internal-open (r/atom false)
        trigger-ref (r/atom nil)
        popover-ref (r/atom nil)
        arrow-ref (r/atom nil)]

    (r/create-class
     {:handle-click-outside
      (fn [this e]
        (let [props (r/props this)
              {:keys [is-open on-change close-on-click-outside]
               :or {close-on-click-outside true}} props

              is-open? (if (some? is-open) is-open @internal-open)

              toggle (fn [new-state]
                       (if (some? on-change)
                         (on-change new-state)
                         (reset! internal-open new-state)))]

          (when (and is-open?
                     close-on-click-outside
                     @trigger-ref
                     (not (.contains @trigger-ref e.target))
                     @popover-ref
                     (not (.contains @popover-ref e.target)))
            (toggle false))))

      :component-did-mount
      (fn [this]
        (.addEventListener js/document "mousedown" (.-handle-click-outside this)))

      :component-will-unmount
      (fn [this]
        (.removeEventListener js/document "mousedown" (.-handle-click-outside this)))

      :reagent-render
      (fn [props]
        (let [{:keys [trigger is-open on-change placement arrow class
                      content transition]
               :or {placement :bottom
                    arrow true
                    transition :scale}} props

              is-open? (if (some? is-open) is-open @internal-open)

              toggle (fn [new-state]
                       (if (some? on-change)
                         (on-change new-state)
                         (reset! internal-open new-state)))

              placement-classes
              (case placement
                :top "bottom-full left-1/2 transform -translate-x-1/2 mb-2"
                :right "left-full top-1/2 transform -translate-y-1/2 ml-2"
                :bottom "top-full left-1/2 transform -translate-x-1/2 mt-2"
                :left "right-full top-1/2 transform -translate-y-1/2 mr-2"
                "top-full left-1/2 transform -translate-x-1/2 mt-2")

              arrow-classes
              (case placement
                :top "bottom-[-6px] left-1/2 transform -translate-x-1/2 rotate-45"
                :right "left-[-6px] top-1/2 transform -translate-y-1/2 rotate-45"
                :bottom "top-[-6px] left-1/2 transform -translate-x-1/2 rotate-45"
                :left "right-[-6px] top-1/2 transform -translate-y-1/2 rotate-45"
                "top-[-6px] left-1/2 transform -translate-x-1/2 rotate-45")

              transition-classes
              (case transition
                :fade (if is-open? "opacity-100" "opacity-0")
                :scale (if is-open? "scale-100 opacity-100" "scale-95 opacity-0")
                :slide (case placement
                         :top (if is-open? "translate-y-0 opacity-100" "translate-y-2 opacity-0")
                         :right (if is-open? "translate-x-0 opacity-100" "translate-x-2 opacity-0")
                         :bottom (if is-open? "translate-y-0 opacity-100" "translate-y-2 opacity-0")
                         :left (if is-open? "translate-x-0 opacity-100" "translate-x-2 opacity-0")
                         (if is-open? "translate-y-0 opacity-100" "translate-y-2 opacity-0"))
                (if is-open? "scale-100 opacity-100" "scale-95 opacity-0"))]

          [:div.relative.inline-block
           [:div {:ref #(reset! trigger-ref %)
                  :on-click #(toggle (not is-open?))}
            trigger]

           [:div {:ref #(reset! popover-ref %)
                  :class (str "absolute z-50 "
                              "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] "
                              "shadow-lg rounded-md p-4 min-w-[200px] "
                              "transition-all duration-200 "
                              transition-classes " "
                              placement-classes " "
                              (when-not is-open? "pointer-events-none ")
                              class)}

            (when arrow
              [:div {:ref #(reset! arrow-ref %)
                     :class (str "absolute w-3 h-3 bg-[var(--color-light-card)] "
                                 "dark:bg-[var(--color-dark-card)] "
                                 arrow-classes)}])

            (if (fn? content)
              [content #(toggle false)]
              content)]]))})))

(defn tooltip
  "A simplified tooltip component built on top of popover.
   
   Options:
   - text: tooltip text content
   - placement: :top, :right, :bottom, :left (default :top)
   - delay: delay in ms before showing (default 300)
   - class: additional CSS classes
   - trigger: element that triggers the tooltip"
  [{:keys [delay] :or {delay 300}}]
  (let [show? (r/atom false)
        timer (r/atom nil)

        handle-mouse-enter (fn []
                             (when @timer
                               (js/clearTimeout @timer)
                               (reset! timer nil))
                             (reset! timer (js/setTimeout #(reset! show? true) delay)))

        handle-mouse-leave (fn []
                             (when @timer
                               (js/clearTimeout @timer)
                               (reset! timer nil))
                             (reset! show? false))]

    (fn [{:keys [text placement class trigger]
          :or {placement :top}}]
      [:div.inline-block
       {:on-mouse-enter handle-mouse-enter
        :on-mouse-leave handle-mouse-leave
        :on-focus handle-mouse-enter
        :on-blur handle-mouse-leave}

       [popover {:trigger trigger
                 :is-open @show?
                 :placement placement
                 :arrow true
                 :class (str "py-1 px-2 text-xs " class)
                 :content [:div text]}]])))

(defn menu-item
  "A menu item for use in context menus and dropdown menus.
   
   Options:
   - icon: optional icon component 
   - disabled: true/false (default false)
   - danger: true/false (default false) - use warning/error styling
   - on-click: function to call when clicked
   - class: additional CSS classes"
  [{:keys [icon disabled danger on-click class]} & children]

  (let [disabled-val (or disabled false)
        danger-val (or danger false)

        base-classes "flex items-center w-full px-3 py-2 text-sm transition-colors duration-100 gap-2"

        state-classes
        (cond
          disabled-val "opacity-50 cursor-not-allowed text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"
          danger-val "text-[var(--color-error)] hover:bg-[var(--color-error-50)] dark:text-[var(--color-error-300)] dark:hover:bg-[rgba(244,67,54,0.15)]"
          :else "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] hover:bg-[var(--color-light-bg-paper)] dark:hover:bg-[var(--color-dark-bg-paper)]")

        all-classes (str base-classes " " state-classes " " class)

        handle-click
        (fn [e]
          (when (and on-click (not disabled-val))
            (.stopPropagation e)
            (on-click e)))]

    [:button
     {:class all-classes
      :on-click handle-click}

     (when icon
       [:div
        [:> icon
         {:size 16
          :class (when danger-val "text-[var(--color-error)] dark:text-[var(--color-error-300)]")}]])

     (into [:span] children)]))

(defn menu-divider
  "A horizontal divider for use in menus."
  []
  [:div.h-px.my-1
   {:class "bg-[var(--color-light-divider)] dark:bg-[var(--color-dark-divider)]"}])

(defn context-menu
  "A context menu component that appears on right-click at the cursor position.
   
   Options:
   - is-open: boolean to control visibility (use with on-change for controlled usage)
   - on-change: function to call when open state changes (fn [new-state])
   - x: horizontal position in pixels (required if controlled)
   - y: vertical position in pixels (required if controlled)
   - class: additional CSS classes
   - items: vector of menu item definitions"
  []

  (let [internal-state (r/atom {:open false :x nil :y nil})
        menu-ref (r/atom nil)]

    (r/create-class
     {:display-name "ContextMenu"

      :update-state
      (fn [this new-open-state]
        (let [props (r/props this)
              {:keys [on-change]} props
              controlled? (and (some? (:is-open props)) (some? on-change))]
          (if controlled?
            (on-change new-open-state)
            (swap! internal-state assoc :open new-open-state))))

      :handle-context-menu
      (fn [this e]
        (let [props (r/props this)
              {:keys [is-open on-change]} props
              controlled? (and (some? is-open) (some? on-change))
              new-x (.-clientX e)
              new-y (.-clientY e)]
          (.preventDefault e)
          (if controlled?
            (when on-change
              (on-change {:open true :x new-x :y new-y}))
            (reset! internal-state {:open true :x new-x :y new-y}))))

      :handle-click-outside
      (fn [this e]
        (let [props (r/props this)
              {:keys [is-open]} props
              is-open? (if (some? is-open) is-open (:open @internal-state))]
          (when (and is-open?
                     @menu-ref
                     (not (.contains @menu-ref e.target)))
            ((.-update-state this) false))))

      :close-menu
      (fn [this]
        ((.-update-state this) false))

      :component-did-mount
      (fn [this]
        (let [props (r/props this)
              target (if (:target-ref props)
                       @(:target-ref props)
                       js/document)]
          (when target
            (.addEventListener target "contextmenu" (.-handle-context-menu this)))
          (.addEventListener js/document "mousedown" (.-handle-click-outside this))))

      :component-will-unmount
      (fn [this]
        (let [props (r/props this)
              target (if (:target-ref props)
                       @(:target-ref props)
                       js/document)]
          (when target
            (.removeEventListener target "contextmenu" (.-handle-context-menu this)))
          (.removeEventListener js/document "mousedown" (.-handle-click-outside this))))

      :reagent-render
      (fn [props]
        (let [{:keys [is-open x y class items]
               :or {is-open false}} props

              controlled? (and (some? is-open) (some? (:on-change props)))

              current-is-open? (if controlled? is-open (:open @internal-state))
              position-x (if controlled? x (:x @internal-state))
              position-y (if controlled? y (:y @internal-state))

              render-item (fn [this item idx]
                            (cond
                              (:divider item) [menu-divider {:key idx}]
                              (:label item) [:div {:class "px-3 py-2 text-xs font-semibold text-gray-500" :key idx}
                                             (:label item)]
                              :else [menu-item {:key idx
                                                :icon (:icon item)
                                                :disabled (:disabled item)
                                                :danger (:danger item)
                                                :on-click (fn [e]
                                                            (when (:on-click item)
                                                              ((:on-click item) e))
                                                            ((.-close-menu this) e))}
                                     (:content item)]))]

          (when current-is-open?
            [:div
             {:ref #(reset! menu-ref %)
              :style {:position "fixed"
                      :top position-y
                      :left position-x
                      :z-index 100}
              :class (str "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] " \
                          "shadow-lg rounded-md py-1 min-w-[160px] " \
                          "transition-opacity duration-100 " \
                          (if current-is-open? "opacity-100" "opacity-0") " "
                          class)}
             (map-indexed (partial render-item (r/current-component)) items)])))})))