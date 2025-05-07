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
        (let [{:keys [open? class multi-select? transition trigger-class on-close on-apply trigger]
               :or {multi-select? false
                    transition :fade}}
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
                           :class (cx "dropdown-menu"
                                      (case transition
                                        :scale "animate-in fade-in zoom-in-95"
                                        :slide "animate-in fade-in slide-in-from-top-2"
                                        :fade "animate-in fade-in"
                                        "animate-in fade-in")
                                      class)}
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

          [:div {:class (cx "inline-block" trigger-class)}
           [:div {:ref #(reset! trigger-ref %)
                  :on-click (toggle-dropdown-handler open? on-close)}
            trigger]
           portal-element]))})))

(defn menu-item
  "A menu item component for use within a dropdown.
   
   Options:
   - selected?: true/false - whether this item is selected
   - multi-select?: true/false - whether this item is in a multi-select dropdown
   - disabled?: true/false - whether this item is disabled
   - on-click: function to call when clicked
   - start-icon: icon component to show at start
   - end-icon: icon component to show at end
   - class: additional CSS classes"
  [{:keys [selected? multi-select? disabled? on-click start-icon end-icon class]} & children]
  (let [base-classes "dropdown-item"

        selected-classes (when selected?
                           "dropdown-item-selected")

        disabled-classes (when disabled?
                           "dropdown-item-disabled")

        all-classes (cx base-classes selected-classes disabled-classes class)

        handle-click (fn [e]
                       (when (and on-click (not disabled?))
                         (.stopPropagation e)
                         (on-click e)))]

    [:div {:class all-classes
           :on-click handle-click
           :role "menuitem"
           :tabIndex (if disabled? -1 0)}
     (when start-icon
       [:div {:class "mr-2 flex-shrink-0"}
        [:> start-icon {:size 16}]])

     [:div {:class "flex-grow truncate"}
      (into [:span] children)]

     (cond
       multi-select?
       [:div {:class "ml-2 flex-shrink-0"}
        [:div {:class (cx "w-4 h-4 rounded border flex items-center justify-center"
                          (if selected?
                            "bg-primary-500 border-primary-500"
                            "border-gray-300 dark:border-gray-600"))}
         (when selected?
           [:> lucide-icons/Check {:size 12 :className "text-white"}])]]

       selected?
       [:div {:class "ml-2 flex-shrink-0 text-primary-500"}
        [:> lucide-icons/Check {:size 16}]]

       end-icon
       [:div {:class "ml-2 flex-shrink-0"}
        [:> end-icon {:size 16}]])]))

(defn menu-label
  "A label component for grouping menu items in a dropdown.
   
   Options:
   - class: additional CSS classes"
  [{:keys [class]} & children]
  [:div {:class (cx "px-2 py-1.5 text-xs font-medium text-gray-500 dark:text-gray-400" class)}
   (into [:span] children)])

(defn menu-divider
  "A separator line for visually dividing menu items.
   
   Options:
   - class: additional CSS classes"
  [{:keys [class]}]
  [:div {:class (cx "h-px my-1 bg-gray-200 dark:bg-gray-700" class)}])