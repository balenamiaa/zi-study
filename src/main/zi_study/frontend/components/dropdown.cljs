(ns zi-study.frontend.components.dropdown
  (:require
   [reagent.core :as r]
   ["react-dom" :as react-dom]
   ["lucide-react" :as lucide-icons]))

(defn dropdown [{:keys [placement width transition class trigger-class on-close
                        multi-select? selected-values on-apply]
                 :or {placement :bottom-left
                      width "w-48"
                      transition :fade
                      multi-select? false
                      selected-values #{}}}
                & children]
  (let [open (r/atom false)
        portal-container (r/atom nil)
        trigger-ref (r/atom nil)
        dropdown-ref (r/atom nil)
        trigger-rect (r/atom nil)
        temp-selections (r/atom (or selected-values #{}))

        update-position (fn []
                          (when (and @trigger-ref @dropdown-ref @open)
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
                              (reset! trigger-rect rect)
                              (set! (.. dropdown-el -style -position) "absolute")
                              (set! (.. dropdown-el -style -zIndex) "9999")
                              (set! (.. dropdown-el -style -left) (str (:left position) "px"))
                              (set! (.. dropdown-el -style -top) (str (:top position) "px"))
                              (set! (.. dropdown-el -style -width) width))))

        handle-outside-click (fn [e]
                               (when (and @open
                                          @dropdown-ref
                                          (not (.contains @dropdown-ref (.-target e)))
                                          @trigger-ref
                                          (not (.contains @trigger-ref (.-target e))))
                                 (reset! open false)
                                 (reset! temp-selections selected-values)
                                 (when on-close (on-close))))

        toggle-dropdown (fn [e]
                          (.stopPropagation e)
                          (swap! open not)
                          (when @open
                            (js/setTimeout update-position 0))
                          (when (and (not @open) on-close)
                            (on-close)))

        handle-apply (fn [e]
                       (.stopPropagation e)
                       (when on-apply (on-apply @temp-selections))
                       (reset! open false)
                       (when on-close (on-close)))

        handle-clear (fn [e]
                       (.stopPropagation e)
                       (reset! temp-selections #{})
                       (when on-apply (on-apply #{})))

        transition-classes (case transition
                             :fade "transition-opacity duration-200 ease-in-out"
                             :scale "transition-transform duration-200 ease-in-out transform origin-top-left scale-95 opacity-0 group-data-[state=open]:scale-100 group-data-[state=open]:opacity-100"
                             :slide "transition-all duration-200 ease-in-out transform -translate-y-2 opacity-0 group-data-[state=open]:translate-y-0 group-data-[state=open]:opacity-100"
                             "transition-opacity duration-200 ease-in-out")

        create-portal (fn []
                        (when @open
                          (react-dom/createPortal
                           (r/as-element
                            [:div {:ref #(reset! dropdown-ref %)
                                   :class (str "bg-white dark:bg-[var(--color-dark-card)] rounded-md shadow-lg "
                                               "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] "
                                               transition-classes " " class " "
                                               (if @open "opacity-100 visible" "opacity-0 invisible pointer-events-none"))}

                             [:div {:class "py-1 max-h-60 overflow-y-auto scrollbar-thin"}
                              (map-indexed
                               (fn [idx child]
                                 (if (vector? child)
                                   (if multi-select?
                                     (vary-meta
                                      (update child 1 assoc
                                              :multi-select? true
                                              :selected? (contains? @temp-selections (:value (second child)))
                                              :on-toggle #(swap! temp-selections
                                                                 (if (contains? @temp-selections %)
                                                                   disj
                                                                   conj) %))
                                      assoc :key idx)
                                     (vary-meta child assoc :key idx))
                                   child))
                               children)]

                             (when multi-select?
                               [:div {:class (str "border-t border-[var(--color-light-divider)] "
                                                  "dark:border-[var(--color-dark-divider)] p-2 flex justify-between")}
                                [:button {:class (str "text-xs px-2 py-1 rounded-md "
                                                      "text-[var(--color-light-text-secondary)] "
                                                      "dark:text-[var(--color-dark-text-secondary)] "
                                                      "hover:bg-[var(--color-light-bg-paper)] "
                                                      "dark:hover:bg-[var(--color-dark-bg-paper)]")
                                          :on-click handle-clear}
                                 "Clear"]
                                [:button {:class (str "text-xs px-3 py-1 rounded-md bg-[var(--color-primary)] "
                                                      "text-white hover:bg-[var(--color-primary-600)] "
                                                      "transition-colors duration-150")
                                          :on-click handle-apply}
                                 "Apply"]])])
                           @portal-container)))]

    (r/create-class
     {:display-name "PortalDropdown"

      :component-did-mount
      (fn [_]
        (let [container (js/document.createElement "div")]
          (js/document.body.appendChild container)
          (reset! portal-container container)
          (js/document.addEventListener "mousedown" handle-outside-click)
          (js/window.addEventListener "resize" update-position)
          (js/window.addEventListener "scroll" update-position true)))

      :component-did-update
      (fn [_ _]
        (when @open
          (update-position)))

      :component-will-unmount
      (fn [_]
        (js/document.removeEventListener "mousedown" handle-outside-click)
        (js/window.removeEventListener "resize" update-position)
        (js/window.removeEventListener "scroll" update-position true)
        (when @portal-container
          (js/document.body.removeChild @portal-container)
          (reset! portal-container nil)))

      :reagent-render
      (fn [{:keys [trigger]} & _]
        [:div {:class "relative inline-block"
               :data-state (if @open "open" "closed")}

         [:div {:ref #(reset! trigger-ref %)
                :class (str "inline-block cursor-pointer " trigger-class)
                :on-click toggle-dropdown}
          trigger]

         (create-portal)])})))

(defn menu-item [{:keys [icon disabled danger on-click class value multi-select? selected? on-toggle]} & children]
  (let [disabled-val (or disabled false)
        danger-val (or danger false)

        base-classes "flex items-center w-full px-4 py-2 text-sm transition-colors duration-150 gap-2"

        state-classes (cond
                        disabled-val "opacity-50 cursor-not-allowed text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"
                        danger-val "text-[var(--color-error)] hover:bg-[var(--color-error-50)] dark:text-[var(--color-error-300)] dark:hover:bg-[rgba(244,67,54,0.15)]"
                        selected? "bg-[var(--color-primary-50)] text-[var(--color-primary-700)] dark:bg-[rgba(233,30,99,0.15)] dark:text-[var(--color-primary-300)]"
                        :else "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] hover:bg-[var(--color-light-bg-paper)] dark:hover:bg-[var(--color-dark-bg-paper)]")

        all-classes (str base-classes " " state-classes " " class)

        handle-click (fn [e]
                       (when (not disabled-val)
                         (.stopPropagation e)
                         (if multi-select?
                           (when on-toggle
                             (on-toggle value))
                           (when on-click
                             (on-click e)))))]

    [:button
     {:class all-classes
      :on-click handle-click}

     (cond
       multi-select?
       [:div {:class (str "flex items-center justify-center w-4 h-4 rounded border transition-colors duration-150 dark:border-[var(--color-dark-divider)]"
                          (when selected? " bg-[var(--color-primary)] border-[var(--color-primary)]"))}
        (when selected?
          [:> lucide-icons/Check {:size 12 :class "text-white"}])]

       icon
       [:div
        [:> icon
         {:size 16
          :class (when danger-val "text-[var(--color-error)] dark:text-[var(--color-error-300)]")}]])

     (into [:span] children)]))

(defn menu-divider []
  [:div {:class "h-px my-1 bg-[var(--color-light-divider)] dark:bg-[var(--color-dark-divider)]"}])

(defn menu-label [{:keys [class]} & children]
  [:div
   {:class (str "px-4 py-2 text-xs font-semibold text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] " class)}
   (into [:span] children)])