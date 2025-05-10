(ns zi-study.frontend.components.tooltip
  (:require
   [reagent.core :as r]
   ["react-dom" :as react-dom]
   [zi-study.frontend.utilities :refer [cx]]))

(defn tooltip
  "A tooltip component that displays information when hovering over elements.
   
   Options:
   - content: The text or hiccup to display in the tooltip (required)
   - position: :top, :right, :bottom, :left (default :top)
   - delay: Time in ms before showing tooltip (default 300)
   - disabled: true/false (default false)
   - max-width: Maximum width of tooltip (default '250px')
   - class: Additional CSS classes for the tooltip
   - arrow: true/false whether to show a directional arrow (default true)
   - variant: :light, :dark (default :dark) - tooltip theme variant
   - trigger-class: Additional classes for the trigger wrapper"
  [{:keys [position delay disabled variant]
    :or {position :top
         delay 300
         disabled false
         variant :dark}}]

  (let [show? (r/atom false)
        trigger-ref (r/atom nil)
        tooltip-ref (r/atom nil)
        portal-container (r/atom nil)
        timer (r/atom nil)

        show-tooltip (fn []
                       (when-not disabled
                         (when @timer
                           (js/clearTimeout @timer))
                         (reset! timer (js/setTimeout #(reset! show? true) delay))))

        hide-tooltip (fn []
                       (when @timer
                         (js/clearTimeout @timer))
                       (reset! timer nil)
                       (reset! show? false))

        ;; Simplified position-class: JS will handle precise pixel positioning.
        ;; We only keep classes that don't interfere with left/top calculations.
        position-class (case position
                         :top "mb-8"    ; Increased margin for better spacing from trigger
                         :right "ml-2"   ; Margin for spacing from trigger
                         :bottom "mt-2"  ; Margin for spacing from trigger
                         :left "mr-2"    ; Margin for spacing from trigger
                         "mb-8")     ; Default margin for spacing

        arrow-class (case position
                      :top "bottom-0 left-1/2 -translate-x-1/2 translate-y-full border-t-current border-l-transparent border-r-transparent border-b-transparent"
                      :right "left-0 top-1/2 -translate-y-1/2 -translate-x-full border-r-current border-t-transparent border-b-transparent border-l-transparent"
                      :bottom "top-0 left-1/2 -translate-x-1/2 -translate-y-full border-b-current border-l-transparent border-r-transparent border-t-transparent"
                      :left "right-0 top-1/2 -translate-y-1/2 translate-x-full border-l-current border-t-transparent border-b-transparent border-r-transparent"
                      "bottom-0 left-1/2 -translate-x-1/2 translate-y-full border-t-current border-l-transparent border-r-transparent border-b-transparent")

        variant-class (case variant
                        :light "bg-white text-[var(--color-light-text-primary)] border border-[var(--color-light-divider)] shadow-md dark:bg-[var(--color-dark-bg-paper)] dark:text-[var(--color-dark-text-primary)] dark:border-[var(--color-dark-divider)]"
                        :dark "bg-[#222] text-white border-none shadow-lg"
                        "bg-[#222] text-white border-none shadow-lg")

        update-position (fn []
                          (when (and @trigger-ref @tooltip-ref @show?)
                            (js/requestAnimationFrame
                             (fn []
                               (let [rect (.getBoundingClientRect @trigger-ref)
                                     tooltip-el @tooltip-ref
                                     scroll-x (.-scrollX js/window)
                                     scroll-y (.-scrollY js/window)
                                     tooltip-width (.-offsetWidth tooltip-el)
                                     tooltip-height (.-offsetHeight tooltip-el)

                                     ;; Rect values are relative to viewport
                                     left (.-left rect)
                                     right (.-right rect)
                                     top (.-top rect)
                                     bottom (.-bottom rect)
                                     trigger-width (.-width rect)
                                     trigger-height (.-height rect)

                                     ;; Calculate positions with correct scroll offsets
                                     ;; getBoundingClientRect returns viewport coordinates, so we add scroll
                                     ;; to convert to absolute document coordinates
                                     position-map {:top {:left (+ left scroll-x (/ trigger-width 2) (- (/ tooltip-width 2)))
                                                         :top (- (+ top scroll-y) tooltip-height 8)} ;; Position above the element with 8px gap
                                                   :right {:left (+ right scroll-x 8)
                                                           :top (+ top scroll-y (/ trigger-height 2) (- (/ tooltip-height 2)))}
                                                   :bottom {:left (+ left scroll-x (/ trigger-width 2) (- (/ tooltip-width 2)))
                                                            :top (+ bottom scroll-y 8)}
                                                   :left {:left (+ left scroll-x (- tooltip-width 8))
                                                          :top (+ top scroll-y (/ trigger-height 2) (- (/ tooltip-height 2)))}}

                                     ;; Get the position for the current direction
                                     coords (get position-map position)

                                     ;; Boundary checking to keep tooltip in viewport
                                     window-width (.-innerWidth js/window)
                                     window-height (.-innerHeight js/window)
                                     adjusted-left (-> (:left coords)
                                                       (max (+ scroll-x 8))
                                                       (min (+ scroll-x window-width (- tooltip-width 8))))
                                     adjusted-top (-> (:top coords)
                                                      (max (+ scroll-y 8))
                                                      (min (+ scroll-y window-height (- tooltip-height 8))))]

                                 (set! (.. tooltip-el -style -position) "absolute")
                                 (set! (.. tooltip-el -style -zIndex) "9999")
                                 (set! (.. tooltip-el -style -left) (str adjusted-left "px"))
                                 (set! (.. tooltip-el -style -top) (str adjusted-top "px")))))))]

    (r/create-class
     {:display-name "Tooltip"

      :component-did-mount
      (fn [_]
        (let [container (js/document.createElement "div")]
          (js/document.body.appendChild container)
          (reset! portal-container container)
          (js/window.addEventListener "resize" update-position)
          (js/window.addEventListener "scroll" update-position true)))

      :component-did-update
      (fn [this old-argv]
        (let [[_ _] old-argv
              [_ _] (r/argv this)]
          (when @show?
            (update-position))))

      :component-will-unmount
      (fn [_]
        (when @timer
          (js/clearTimeout @timer)
          (reset! timer nil))
        (js/window.removeEventListener "resize" update-position)
        (js/window.removeEventListener "scroll" update-position true)
        (when @portal-container
          (js/document.body.removeChild @portal-container)
          (reset! portal-container nil)))

      :reagent-render
      (fn [{:keys [content position max-width class arrow trigger-class] :or {max-width "250px" arrow true}} & children]
        (let [tooltip-content (when @show?
                                (react-dom/createPortal
                                 (r/as-element
                                  [:div {:ref #(reset! tooltip-ref %)
                                         :class (cx "absolute pointer-events-auto rounded px-2 py-1 text-sm animate-in zoom-in-90 fade-in duration-200"
                                                    position-class ; Uses the simplified position-class
                                                    variant-class
                                                    class)
                                         :style {:max-width (or max-width "250px")}}
                                   ;; Arrow
                                   (when arrow
                                     [:span {:class (cx "absolute w-0 h-0 border-4" arrow-class)
                                             :style {:border-top-color (when (= position :top) "currentColor")
                                                     :border-right-color (when (= position :right) "currentColor")
                                                     :border-bottom-color (when (= position :bottom) "currentColor")
                                                     :border-left-color (when (= position :left) "currentColor")}}])

                                   ;; Content
                                   (if (string? content)
                                     [:span content]
                                     content)])
                                 @portal-container))]

          [:div {:ref #(reset! trigger-ref %)
                 :class (cx "inline-flex w-full" trigger-class)
                 :on-mouse-enter show-tooltip
                 :on-mouse-leave hide-tooltip
                 :on-focus show-tooltip
                 :on-blur hide-tooltip}

           ;; Trigger element(s)
           (into [:<>] children)

           ;; Tooltip portal
           tooltip-content]))})))