(ns zi-study.frontend.components.dropdown
  (:require
   [reagent.core :as r]
   ["react-dom" :as react-dom]
   ["lucide-react" :as lucide-icons]))

(defn dropdown
  "A dropdown menu component with consistent width handling.
   
   Options:
   - open?: atom containing boolean state (default: new atom with false)
   - placement: :bottom-left, :bottom-right, :top-left, :top-right (default :bottom-left)
   - width: Set dropdown width - string CSS value, or :match-trigger to match trigger width (default \"w-48\")
   - min-width: Minimum width for the dropdown (optional)
   - multi-select?: true/false to enable multi-select mode (default false)
   - trigger: component that triggers the dropdown
   - trigger-class: additional CSS classes for trigger container
   - on-close: callback when dropdown closes
   - on-apply: callback when 'Apply' is clicked in multi-select mode
   - selected-values: set of initially selected values for multi-select mode
   - class: additional CSS classes for dropdown content"
  [{:keys [open? placement width min-width selected-values]
    :or {open? (r/atom false)
         placement :bottom-left
         width "w-48"
         min-width nil
         selected-values #{}}}
   & _children-initial]

  (let [portal-container (r/atom nil)
        trigger-ref (r/atom nil)
        dropdown-ref (r/atom nil)
        trigger-width (r/atom nil)  ;; Track the trigger width
        temp-selections (r/atom (or selected-values #{}))
        prev-open-state (r/atom @open?)
        force-update-portal (r/atom 0)
        memoized-outside-click-handler (r/atom nil)

        update-position (fn []
                          (when (and @trigger-ref @dropdown-ref @open?)
                            (let [rect (.getBoundingClientRect @trigger-ref)
                                  dropdown-el @dropdown-ref
                                  dropdown-width (.-offsetWidth dropdown-el)
                                  dropdown-height (.-offsetHeight dropdown-el)
                                  scroll-x (.-scrollX js/window)
                                  scroll-y (.-scrollY js/window)
                                  left (.-left rect)
                                  right (.-right rect)
                                  top (.-top rect)
                                  bottom (.-bottom rect)

                                  ;; Update stored trigger width on position update
                                  trigger-element-width (.-offsetWidth @trigger-ref)
                                  _ (reset! trigger-width trigger-element-width)

                                  position (case placement
                                             :bottom-left {:left (+ left scroll-x)
                                                           :top (+ bottom scroll-y)}
                                             :bottom-right {:left (+ (- right dropdown-width) scroll-x)
                                                            :top (+ bottom scroll-y)}
                                             :top-left {:left (+ left scroll-x)
                                                        :top (+ (- top dropdown-height) scroll-y)}
                                             :top-right {:left (+ (- right dropdown-width) scroll-x)
                                                         :top (+ (- top dropdown-height) scroll-y)}
                                             {:left (+ left scroll-x)
                                              :top (+ bottom scroll-y)})]
                              (set! (.. dropdown-el -style -position) "absolute")
                              (set! (.. dropdown-el -style -zIndex) "9999")
                              (set! (.. dropdown-el -style -left) (str (:left position) "px"))
                              (set! (.. dropdown-el -style -top) (str (:top position) "px"))

                              ;; Match dropdown width to trigger width if specified, otherwise use provided width
                              (if (= width :match-trigger)
                                (set! (.. dropdown-el -style -width) (str @trigger-width "px"))
                                (set! (.. dropdown-el -style -width) width))

                              ;; Apply minimum width if specified
                              (when min-width
                                (set! (.. dropdown-el -style -minWidth) min-width)))))

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
                                      (js/setTimeout update-position 0))
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

          (when @(:open? current-props)
            (update-position))))

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
        (let [{:keys [open? class multi-select? transition trigger-class on-close on-apply trigger width min-width]
               :or {multi-select? false
                    transition :fade
                    width "w-48"
                    min-width nil}}
              props-render

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
                           :class (str "dropdown-menu "
                                       (case transition
                                         :fade "transition-opacity duration-200 ease-in-out"
                                         :scale "transition-transform duration-200 ease-in-out transform origin-top-left scale-95 opacity-0 group-data-[state=open]:scale-100 group-data-[state=open]:opacity-100"
                                         :slide "transition-all duration-200 ease-in-out transform -translate-y-2 opacity-0 group-data-[state=open]:translate-y-0 group-data-[state=open]:opacity-100"
                                         "transition-opacity duration-200 ease-in-out")
                                       " " class " "
                                       (if @open? "opacity-100 visible" "opacity-0 invisible pointer-events-none"))}
                     [:div {:class "py-1 max-h-60 overflow-y-auto scrollbar-thin"}
                      processed-children]

                     (when multi-select?
                       [:div {:class "border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] p-2 flex justify-between"}
                        [:button {:class (str "text-xs px-2 py-1 rounded-md "
                                              "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] "
                                              "hover:bg-[var(--color-light-bg-paper)] dark:hover:bg-[var(--color-dark-bg-paper)] transition-colors duration-150")
                                  :on-click (handle-clear-handler on-apply)}
                         "Clear"]
                        [:button {:class (str "text-xs px-3 py-1 rounded-md bg-[var(--color-primary)] text-white "
                                              "hover:bg-[var(--color-primary-600)] focus:ring-2 focus:ring-[var(--color-primary-300)] "
                                              "active:translate-y-px transition-all duration-150")
                                  :on-click (handle-apply-handler on-apply open? on-close)}
                         "Apply"]])])
                   @portal-container)))]

          [:div {:class (str "relative inline-block" (when @open? " dropdown-open"))
                 :data-state (if @open? "open" "closed")}
           [:div {:ref #(reset! trigger-ref %)
                  :class (str "inline-block cursor-pointer " trigger-class)
                  :on-click (toggle-dropdown-handler open? on-close)}
            trigger]
           portal-element]))})))

(defn menu-item
  "Represents a clickable item within a dropdown menu.
   
   Props:
   - :icon - Lucide icon component to display
   - :start-icon - Icon component to display at the start of the item
   - :end-icon - Icon component to display at the end of the item
   - :disabled - Whether the item is disabled
   - :danger - Whether the item represents a dangerous action (shown in red)
   - :on-click - Function called when the item is clicked
   - :class - Additional CSS classes for the item
   - :value - Value of the item (used for multi-select)
   - :multi-select? - Whether this item is part of a multi-select dropdown
   - :selected? - Whether this item is currently selected"
  [{:keys [icon disabled danger on-click class value multi-select? selected?
           start-icon end-icon]} & children]
  (let [disabled-val (or disabled false)
        danger-val (or danger false)

        ;; Use the new dropdown-item classes
        base-classes "dropdown-item"

        ;; Add modifier classes
        modifier-classes (str
                          (when selected? " selected")
                          (when disabled-val " disabled")
                          (when danger-val " danger")
                          " " class)

        handle-click (fn [e]
                       (when (not disabled-val)
                         (.stopPropagation e)
                         (when on-click
                           (if value
                             (on-click value e)
                             (on-click e)))))]

    [:button
     {:class (str base-classes modifier-classes)
      :on-click handle-click
      :disabled disabled-val
      :aria-disabled disabled-val
      :role "menuitem"
      :tabIndex (if disabled-val "-1" "0")}

     ;; Start icon if provided
     (when start-icon
       [:> start-icon {:size 16 :className "flex-shrink-0 mr-2"}])

     ;; Icon from icon prop (for backward compatibility)
     (when (and icon (not start-icon))
       [:> icon {:size 16 :className "flex-shrink-0 mr-2"}])

     ;; Multi-select checkbox
     (when multi-select?
       [:div {:class (str "flex items-center justify-center w-4 h-4 mr-2 rounded border "
                          "transition-colors duration-150 "
                          (if selected?
                            "bg-[var(--color-primary)] border-[var(--color-primary)]"
                            "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"))}
        (when selected?
          [:> lucide-icons/Check {:size 12 :class "text-white"}])])

     ;; Item content
     [:span {:class "flex-grow"} (into [:span] children)]

     ;; End icon if provided
     (when end-icon
       [:> end-icon {:size 16 :className "flex-shrink-0 ml-auto"}])]))

(defn menu-divider
  "A simple horizontal divider for visually separating groups of menu items." []
  [:div {:class "h-px my-1 bg-[var(--color-light-divider)] dark:bg-[var(--color-dark-divider)]"
         :role "separator"
         :aria-orientation "horizontal"}])

(defn menu-label
  "A non-interactive label for describing groups of menu items.
   
   Props:
   - :class - Additional CSS classes for the label" [{:keys [class]} & children]
  [:div
   {:class (str "px-4 py-2 text-xs font-semibold uppercase tracking-wider "
                "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] "
                class)
    :role "presentation"}
   (into [:span] children)])