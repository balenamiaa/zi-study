(ns zi-study.frontend.components.dropdown
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]))

(defn dropdown
  "A dropdown menu component.
   
   Options:
   - trigger: required React element that triggers the dropdown
   - placement: :bottom-left, :bottom-right, :top-left, :top-right (default :bottom-left)
   - width: CSS width value (default 'w-48')
   - transition: :fade, :scale, :slide (default :fade)
   - class: additional CSS classes for the dropdown container
   - trigger-class: additional CSS classes for the trigger wrapper
   - on-close: function to call when dropdown closes"
  [{:keys [trigger placement width transition class trigger-class on-close]
    :or {placement :bottom-left
         width "w-48"
         transition :fade}}
   & children]
  
  (let [open (r/atom false)
        dropdown-ref (r/atom nil)
        trigger-ref (r/atom nil)
        
        handle-outside-click (fn [e]
                               (when (and @open
                                          @dropdown-ref
                                          (not (.contains @dropdown-ref (.-target e)))
                                          @trigger-ref
                                          (not (.contains @trigger-ref (.-target e))))
                                 (reset! open false)
                                 (when on-close
                                   (on-close))))
        
        toggle-dropdown (fn [e]
                          (.stopPropagation e)
                          (swap! open not)
                          (when (and (not @open) on-close)
                            (on-close)))
        
        placement-classes
        (case placement
          :bottom-left "left-0 top-full mt-1"
          :bottom-right "right-0 top-full mt-1"
          :top-left "left-0 bottom-full mb-1"
          :top-right "right-0 bottom-full mb-1"
          "left-0 top-full mt-1")
        
        transition-classes
        (case transition
          :fade "transition-opacity duration-200 ease-in-out"
          :scale "transition-transform duration-200 ease-in-out transform origin-top-right scale-95 group-data-[state=open]:scale-100"
          :slide "transition-all duration-200 ease-in-out transform -translate-y-2 group-data-[state=open]:translate-y-0 opacity-0 group-data-[state=open]:opacity-100"
          "transition-opacity duration-200 ease-in-out")]
    
    (r/create-class
     {:display-name "Dropdown"
      
      :component-did-mount
      (fn [_]
        (js/document.addEventListener "mousedown" handle-outside-click))
      
      :component-will-unmount
      (fn [_]
        (js/document.removeEventListener "mousedown" handle-outside-click))
      
      :reagent-render
      (fn [{:keys [trigger]} & children]
        [:div {:class "relative inline-block"
               :data-state (if @open "open" "closed")}
         
         ;; Trigger element
         [:div {:ref #(reset! trigger-ref %)
                :class (str "inline-block cursor-pointer " trigger-class)
                :on-click toggle-dropdown}
          trigger]
         
         ;; Dropdown menu
         [:div {:ref #(reset! dropdown-ref %)
                :class (str "absolute z-50 bg-white dark:bg-[var(--color-dark-card)] rounded-md shadow-lg py-1 "
                            "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] "
                            width " " placement-classes " " transition-classes " " class " "
                            (if @open "opacity-100 visible" "opacity-0 invisible pointer-events-none"))}
          (map-indexed
           (fn [idx child]
             (if (vector? child)
               (vary-meta child assoc :key idx)
               child))
           children)]])})))

(defn menu-item
  "A menu item for use in dropdowns.
   
   Options:
   - icon: optional icon component
   - disabled: true/false (default false)
   - danger: true/false (default false) - use warning/error styling
   - on-click: function to call when clicked
   - class: additional CSS classes"
  [{:keys [icon disabled danger on-click class]} & children]

  (let [disabled-val (or disabled false)
        danger-val (or danger false)

        base-classes "flex items-center w-full px-4 py-2 text-sm transition-colors duration-150 gap-2"

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
  "A horizontal divider for use in dropdown menus."
  []
  [:div {:class "h-px my-1 bg-[var(--color-light-divider)] dark:bg-[var(--color-dark-divider)]"}])

(defn menu-label
  "A label for use in dropdown menus.
   
   Options:
   - class: additional CSS classes"
  [{:keys [class]} & children]

  [:div
   {:class (str "px-4 py-2 text-xs font-semibold text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] "
                class)}
   (into [:span] children)]) 