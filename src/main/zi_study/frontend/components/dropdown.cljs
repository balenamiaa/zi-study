(ns zi-study.frontend.components.dropdown
  (:require
   [reagent.core :as r]
   ["react-dom" :as react-dom]
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.utilities :refer [cx]]))

(defn dropdown
  "A dropdown menu component with consistent width handling.
   
   Options:
   - open?: atom containing boolean state (default: new atom with false)
   - placement: :bottom-left, :bottom-right, :bottom-center, :top-left, :top-right, :top-center (default :bottom-left)
   - width: Tailwind CSS width class (e.g., \"w-48\", \"w-64\"), or :match-trigger to match trigger width (default \"w-48\")
   - min-width: Minimum width for the dropdown (Tailwind class or CSS value) (optional)
   - offset: Pixel offset from the trigger, primarily affecting the top position (default 4)
   - multi-select?: true/false to enable multi-select mode (default false)
   - trigger: component that triggers the dropdown
   - trigger-class: additional CSS classes for trigger container
   - on-open: callback when dropdown opens
   - on-close: callback when dropdown closes
   - on-apply: callback when 'Apply' is clicked in multi-select mode
   - selected-values: set of initially selected values for multi-select mode
   - class: additional CSS classes for dropdown content"
  [{:keys [open? placement width min-width selected-values offset]
    :or {open? (r/atom false)
         placement :bottom-left
         width "w-48"
         min-width nil
         offset 4
         selected-values #{}}}
   & _children-initial]

  (let [portal-container (r/atom nil)
        trigger-ref (r/atom nil)
        dropdown-ref (r/atom nil)
        trigger-width-cache (r/atom nil)
        temp-selections (r/atom (or selected-values #{}))
        prev-open-state (r/atom @open?)
        force-update-portal (r/atom 0)
        memoized-outside-click-handler (r/atom nil)

        update-position (fn []
                          (when (and @trigger-ref @dropdown-ref @open?)
                            (let [dropdown-el @dropdown-ref]
                              ;; Ensure basic styles for reliable measurement:
                              (set! (.. dropdown-el -style -position) "absolute")
                              (set! (.. dropdown-el -style -width) "auto")

                              (let [trigger-element-rect (.getBoundingClientRect @trigger-ref)
                                    scroll-x (.-scrollX js/window)
                                    scroll-y (.-scrollY js/window)
                                    trigger-width (.-width trigger-element-rect)
                                    window-width (.-innerWidth js/window)
                                    window-height (.-innerHeight js/window)]

                                (when (= width :match-trigger)
                                  (let [current-trigger-offset-width (.-offsetWidth @trigger-ref)]
                                    (reset! trigger-width-cache current-trigger-offset-width)
                                    (set! (.. dropdown-el -style -width) (str current-trigger-offset-width "px"))))

                                (let [dropdown-measurement-rect (.getBoundingClientRect dropdown-el)
                                      dropdown-width (.-width dropdown-measurement-rect)
                                      dropdown-height (.-height dropdown-measurement-rect)
                                      left (.-left trigger-element-rect)
                                      right (.-right trigger-element-rect)
                                      top (.-top trigger-element-rect)
                                      bottom (.-bottom trigger-element-rect)
                                      center-x (+ left (/ trigger-width 2))
                                      base-coords (case placement
                                                    :bottom-left {:left (+ left scroll-x)
                                                                  :top (+ bottom scroll-y offset)}
                                                    :bottom-center {:left (+ center-x scroll-x (- (/ dropdown-width 2)))
                                                                    :top (+ bottom scroll-y offset)}
                                                    :bottom-right {:left (+ right scroll-x (- dropdown-width))
                                                                   :top (+ bottom scroll-y offset)}
                                                    :top-left {:left (+ left scroll-x)
                                                               :top (+ top scroll-y (- offset) (- dropdown-height))}
                                                    :top-center {:left (+ center-x scroll-x (- (/ dropdown-width 2)))
                                                                 :top (+ top scroll-y (- offset) (- dropdown-height))}
                                                    :top-right {:left (+ right scroll-x (- dropdown-width))
                                                                :top (+ top scroll-y (- offset) (- dropdown-height))}
                                                    ;; Default case
                                                    {:left (+ left scroll-x)
                                                     :top (+ bottom scroll-y offset)})
                                      final-left (-> (:left base-coords)
                                                     (max scroll-x)
                                                     (min (- (+ scroll-x window-width) dropdown-width)))
                                      final-top (-> (:top base-coords)
                                                    (max scroll-y)
                                                    (min (- (+ scroll-y window-height) dropdown-height)))]

                                  (set! (.. dropdown-el -style -position) "absolute")
                                  (set! (.. dropdown-el -style -zIndex) "9999")
                                  (set! (.. dropdown-el -style -left) (str final-left "px"))
                                  (set! (.. dropdown-el -style -top) (str final-top "px"))

                                  (when min-width
                                    (set! (.. dropdown-el -style -minWidth) min-width)))))))

        create-handle-outside-click (fn [current-on-close current-open-atom current-selected-values-prop]
                                      (fn [e]
                                        (when (and @current-open-atom
                                                   @dropdown-ref
                                                   (not (.contains @dropdown-ref (.-target e)))
                                                   @trigger-ref
                                                   (not (.contains @trigger-ref (.-target e))))
                                          (reset! current-open-atom false)
                                          (reset! temp-selections (or current-selected-values-prop #{}))
                                          (when current-on-close (current-on-close)))))

        toggle-dropdown-handler (fn [current-open-atom current-on-close]
                                  (fn [e]
                                    (.stopPropagation e)
                                    (swap! current-open-atom not)
                                    (when @current-open-atom
                                      (js/window.requestAnimationFrame update-position))
                                    (when (and (not @current-open-atom) current-on-close)
                                      (current-on-close))))

        handle-apply-handler (fn [current-on-apply current-open-atom current-on-close]
                               (fn [e]
                                 (.stopPropagation e)
                                 (when current-on-apply (current-on-apply @temp-selections))
                                 (reset! current-open-atom false)
                                 (when current-on-close (current-on-close))))

        handle-clear-handler (fn [current-on-apply]
                               (fn [e]
                                 (.stopPropagation e)
                                 (reset! temp-selections #{})
                                 (when current-on-apply (current-on-apply #{}))))

        toggle-selection-multi (fn [value]
                                 (swap! temp-selections (if (contains? @temp-selections value) disj conj) value)
                                 (swap! force-update-portal inc))]

    (r/create-class
     {:display-name "PortalDropdown"

      :component-did-mount
      (fn [this]
        (let [props (r/props this)
              container (js/document.createElement "div")]
          (js/document.body.appendChild container)
          (reset! portal-container container)

          (let [handler (create-handle-outside-click (:on-close props) (:open? props) (:selected-values props))]
            (reset! memoized-outside-click-handler handler)
            (js/document.addEventListener "mousedown" handler))

          (js/window.addEventListener "resize" update-position)
          (js/window.addEventListener "scroll" update-position true)))

      :component-did-update
      (fn [this _old-argv]
        (let [current-props (r/props this)
              current-open-atom (:open? current-props)
              current-on-close (:on-close current-props)
              was-open @prev-open-state
              is-now-open @current-open-atom]

          (let [new-handler (create-handle-outside-click (:on-close current-props) (:open? current-props) (:selected-values current-props))]
            (when (not= @memoized-outside-click-handler new-handler)
              (when @memoized-outside-click-handler (js/document.removeEventListener "mousedown" @memoized-outside-click-handler))
              (reset! memoized-outside-click-handler new-handler)
              (js/document.addEventListener "mousedown" new-handler)))

          (when (and was-open (not is-now-open) current-on-close)
            (current-on-close))
          (reset! prev-open-state is-now-open)

          (when (and is-now-open (not was-open))
            (js/window.requestAnimationFrame update-position))))

      :component-will-unmount
      (fn [_this]
        (when @memoized-outside-click-handler
          (js/document.removeEventListener "mousedown" @memoized-outside-click-handler)
          (reset! memoized-outside-click-handler nil))
        (js/window.removeEventListener "resize" update-position)
        (js/window.removeEventListener "scroll" update-position true)
        (when @portal-container
          (js/document.body.removeChild @portal-container)
          (reset! portal-container nil)))

      :reagent-render
      (fn [props-render & children-render]
        (let [{:keys [open? class multi-select? transition trigger-class on-open on-close on-apply trigger width]
               :or {multi-select? false
                    transition :fade}}
              props-render

              dropdown-classes (cx
                                "dropdown-menu"
                                (when (and (string? width) (not= width :match-trigger)) width)
                                (case transition
                                  :scale "animate-in fade-in zoom-in-95"
                                  :slide "animate-in fade-in slide-in-from-top-2"
                                  :fade "animate-in fade-in"
                                  "animate-in fade-in")
                                class)

              processed-children
              (doall
               (map-indexed
                (fn [idx child-hiccup]
                  (with-meta
                    (if (vector? child-hiccup)
                      (if multi-select?
                        (let [value (get-in child-hiccup [1 :value])
                              item-selected? (contains? @temp-selections value)]
                          (update-in child-hiccup [1] assoc
                                     :multi-select? true
                                     :selected? item-selected?
                                     :on-click #(toggle-selection-multi value)))
                        child-hiccup)
                      child-hiccup)
                    {:key idx}))
                children-render))

              portal-element
              (when @open?
                (let [_ @force-update-portal]
                  (react-dom/createPortal
                   (r/as-element
                    [:div {:ref #(reset! dropdown-ref %)
                           :class dropdown-classes}
                     (into [:div] processed-children)
                     (when multi-select?
                       [:div {:class "border-t border-gray-200 dark:border-gray-700 p-2 flex justify-between"}
                        [:button {:class "text-xs text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
                                  :on-click (handle-clear-handler on-apply)}
                         "Clear"]
                        [:button {:class "text-xs font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
                                  :on-click (handle-apply-handler on-apply open? on-close)}
                         "Apply"]])])
                   @portal-container)))]

          [:div {:class (cx "inline-block" trigger-class)
                 :ref #(reset! trigger-ref %)
                 :on-click #(do
                              ((toggle-dropdown-handler open? on-close) %)
                              (when on-open (on-open)))}
           trigger
           portal-element]))})))

(defn menu-item
  "A menu item component for use within a dropdown.
   
   Options:
   - selected?: true/false - whether this item is selected
   - multi-select?: true/false - whether this item is in a multi-select dropdown
   - disabled?: true/false - whether this item is disabled
   - danger?: true/false - whether this is a destructive action (red styling)
   - on-click: function to call when clicked
   - start-icon: icon component to show at start
   - end-icon: icon component to show at end
   - class: additional CSS classes"
  [{:keys [selected? multi-select? disabled? danger? on-click start-icon end-icon class]} & children]
  (let [base-classes "flex items-center w-full px-4 py-2.5 text-sm transition-all duration-200 relative group"

        selected-classes (when selected?
                           "bg-[var(--color-primary-50)] text-[var(--color-primary-700)] dark:bg-[#331A2A] dark:text-[var(--color-primary-300)] font-medium")

        disabled-classes (when disabled?
                           "opacity-50 cursor-not-allowed text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]")

        danger-classes (when danger?
                         "text-[var(--color-error)] hover:bg-[var(--color-error-50)] dark:text-[var(--color-error-300)] dark:hover:bg-[rgba(var(--color-error-rgb),0.1)]")

        hover-classes (when-not disabled?
                        "hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)]")

        all-classes (cx base-classes selected-classes disabled-classes danger-classes hover-classes class)

        handle-click (fn [e]
                       (when (and on-click (not disabled?))
                         (.stopPropagation e)
                         (on-click e)))]

    [:div {:class all-classes
           :on-click handle-click
           :role "menuitem"
           :tabIndex (if disabled? -1 0)}

     ;; Start icon with proper alignment
     (when start-icon
       [:div {:class (cx "mr-3 flex items-center flex-shrink-0 transition-colors"
                         (cond
                           danger? "text-[var(--color-error)] dark:text-[var(--color-error-300)] group-hover:text-[var(--color-error-700)] dark:group-hover:text-[var(--color-error-200)]"
                           selected? "text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"
                           :else "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] group-hover:text-[var(--color-primary-600)] dark:group-hover:text-[var(--color-primary-400)]"))}
        [:> start-icon {:size 18}]])

     ;; Main content with proper vertical alignment
     [:div {:class "flex-grow truncate flex items-center"}
      (into [:span] children)]

     ;; End elements (checkbox, checkmark, or custom icon)
     (cond
       multi-select?
       [:div {:class "ml-3 flex items-center flex-shrink-0"}
        [:div {:class (cx "w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all"
                          (if selected?
                            "bg-[var(--color-primary-500)] border-[var(--color-primary-500)] dark:bg-[var(--color-primary-400)] dark:border-[var(--color-primary-400)]"
                            "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-transparent"))}
         (when selected?
           [:> lucide-icons/Check {:size 14 :className "text-white" :strokeWidth 3}])]]

       selected?
       [:div {:class "ml-3 flex items-center flex-shrink-0 text-[var(--color-primary-500)] dark:text-[var(--color-primary-300)]"}
        [:> lucide-icons/Check {:size 18 :strokeWidth 2.5}]]

       end-icon
       [:div {:class (cx "ml-3 flex items-center flex-shrink-0 transition-colors"
                         (cond
                           danger? "text-[var(--color-error)] dark:text-[var(--color-error-300)] group-hover:text-[var(--color-error-700)] dark:group-hover:text-[var(--color-error-200)]"
                           :else "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] group-hover:text-[var(--color-primary-600)] dark:group-hover:text-[var(--color-primary-400)]"))}
        [:> end-icon {:size 18}]])]))

(defn menu-label
  "A label component for grouping menu items in a dropdown.
   
   Options:
   - class: additional CSS classes"
  [{:keys [class]} & children]
  [:div {:class (cx "px-4 py-1.5 text-xs font-medium text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]" class)}
   (into [:span] children)])

(defn menu-divider
  "A separator line for visually dividing menu items.
   
   Options:
   - class: additional CSS classes"
  [{:keys [class]}]
  [:div {:class (cx "h-px my-1.5 bg-[var(--color-light-divider)] dark:bg-[var(--color-dark-divider)]" class)}])