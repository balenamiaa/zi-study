(ns zi-study.frontend.components.alert
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]))

(defn alert
  "A beautiful, customizable alert component.

   Options:
   - variant: :filled, :outlined, :soft (default :soft)
   - color: :primary, :success, :warning, :error, :info (default :info)
   - title: optional alert title
   - icon: optional icon component
   - dismissible: true/false (default false) - adds a dismiss button
   - on-dismiss: function to call when dismissed
   - class: additional CSS classes"
  [{:keys [variant color title icon dismissible on-dismiss class]
    :or {variant :soft
         color :info
         dismissible false}}
   & children]

  (let [visible (r/atom true)

        base-classes "rounded-md overflow-hidden transition-all duration-300 ease-in-out"

        variant-colors
        (case variant
          :filled
          (case color
            :primary "bg-[var(--color-primary)] text-white"
            :success "bg-[var(--color-success)] text-white"
            :warning "bg-[var(--color-warning)] text-white"
            :error "bg-[var(--color-error)] text-white"
            :info "bg-[var(--color-info)] text-white"
            "bg-[var(--color-info)] text-white")

          :outlined
          (case color
            :primary "border border-[var(--color-primary)] text-[var(--color-primary)] dark:text-[var(--color-primary-300)] dark:border-[var(--color-primary-300)]"
            :success "border border-[var(--color-success)] text-[var(--color-success)]"
            :warning "border border-[var(--color-warning)] text-[var(--color-warning)]"
            :error "border border-[var(--color-error)] text-[var(--color-error)]"
            :info "border border-[var(--color-info)] text-[var(--color-info)]"
            "border border-[var(--color-info)] text-[var(--color-info)]")

          :soft
          (case color
            :primary "bg-[var(--color-primary-50)] text-[var(--color-primary-700)] dark:bg-[rgba(233,30,99,0.15)] dark:text-[var(--color-primary-300)]"
            :success "bg-green-50 text-green-700 dark:bg-[rgba(76,175,80,0.15)] dark:text-green-300"
            :warning "bg-orange-50 text-orange-700 dark:bg-[rgba(255,152,0,0.15)] dark:text-orange-300"
            :error "bg-red-50 text-red-700 dark:bg-[rgba(244,67,54,0.15)] dark:text-red-300"
            :info "bg-blue-50 text-blue-700 dark:bg-[rgba(33,150,243,0.15)] dark:text-blue-300"
            "bg-blue-50 text-blue-700 dark:bg-[rgba(33,150,243,0.15)] dark:text-blue-300")

          "bg-blue-50 text-blue-700 dark:bg-[rgba(33,150,243,0.15)] dark:text-blue-300")

        icon-color
        (case variant
          :filled "text-white"
          :outlined
          (case color
            :primary "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"
            :success "text-[var(--color-success)]"
            :warning "text-[var(--color-warning)]"
            :error "text-[var(--color-error)]"
            :info "text-[var(--color-info)]"
            "text-[var(--color-info)]")
          :soft
          (case color
            :primary "text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"
            :success "text-green-700 dark:text-green-300"
            :warning "text-orange-700 dark:text-orange-300"
            :error "text-red-700 dark:text-red-300"
            :info "text-blue-700 dark:text-blue-300"
            "text-blue-700 dark:text-blue-300")
          "text-blue-700 dark:text-blue-300")

        default-icon
        (case color
          :primary lucide-icons/Info
          :success lucide-icons/CheckCircle
          :warning lucide-icons/AlertTriangle
          :error lucide-icons/AlertCircle
          :info lucide-icons/Info
          lucide-icons/Info)

        used-icon (or icon default-icon)

        all-classes (str base-classes " " variant-colors " " (when-not @visible "opacity-0 max-h-0 py-0") " " class)

        handle-dismiss (fn [_]
                         (reset! visible false)
                         (when on-dismiss
                           (on-dismiss)))]

    (if @visible
      [:div {:class all-classes}
       [:div {:class "p-4 flex gap-3"}

        ;; Icon
        [:div {:class icon-color}
         [:> used-icon {:size 24}]]

        ;; Content
        [:div {:class "flex-1"}
         (when title
           [:div {:class "font-medium mb-1"} title])

         (into [:div] children)]

        ;; Dismiss button
        (when dismissible
          [:button
           {:class "flex-shrink-0 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:opacity-75"
            :on-click handle-dismiss
            :aria-label "Dismiss"}
           [:> lucide-icons/X {:size 18}]])]]
      [:div])))

(defn toast
  "A toast notification component, designed for temporary notifications.

   Options:
   - variant: :filled, :outlined, :soft (default :soft)
   - color: :primary, :success, :warning, :error, :info (default :info)
   - icon: optional icon component
   - action: optional action component (e.g. button)
   - auto-hide: milliseconds before auto-hiding (default nil, no auto-hide)
   - on-hide: function to call when hidden
   - class: additional CSS classes"
  [{:keys [variant color icon action auto-hide on-hide class]
    :or {variant :soft
         color :info}}
   & children]

  (let [visible (r/atom true)

        base-classes "rounded-md shadow-md overflow-hidden transition-all duration-300 ease-in-out max-w-md"

        variant-colors
        (case variant
          :filled
          (case color
            :primary "bg-[var(--color-primary)] text-white"
            :success "bg-[var(--color-success)] text-white"
            :warning "bg-[var(--color-warning)] text-white"
            :error "bg-[var(--color-error)] text-white"
            :info "bg-[var(--color-info)] text-white"
            "bg-[var(--color-info)] text-white")

          :outlined
          (case color
            :primary "border border-[var(--color-primary)] text-[var(--color-primary)] dark:text-[var(--color-primary-300)] dark:border-[var(--color-primary-300)] bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
            :success "border border-[var(--color-success)] text-[var(--color-success)] bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
            :warning "border border-[var(--color-warning)] text-[var(--color-warning)] bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
            :error "border border-[var(--color-error)] text-[var(--color-error)] bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
            :info "border border-[var(--color-info)] text-[var(--color-info)] bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
            "border border-[var(--color-info)] text-[var(--color-info)] bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]")

          :soft
          (case color
            :primary "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] border-l-4 border-[var(--color-primary)] dark:border-[var(--color-primary-300)]"
            :success "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] border-l-4 border-[var(--color-success)]"
            :warning "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] border-l-4 border-[var(--color-warning)]"
            :error "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] border-l-4 border-[var(--color-error)]"
            :info "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] border-l-4 border-[var(--color-info)]"
            "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] border-l-4 border-[var(--color-info)]")

          "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] border-l-4 border-[var(--color-info)]")

        icon-color
        (case variant
          :filled "text-white"

          :outlined
          (case color
            :primary "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"
            :success "text-[var(--color-success)]"
            :warning "text-[var(--color-warning)]"
            :error "text-[var(--color-error)]"
            :info "text-[var(--color-info)]"
            "text-[var(--color-info)]")

          :soft
          (case color
            :primary "text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"
            :success "text-[var(--color-success)]"
            :warning "text-[var(--color-warning)]"
            :error "text-[var(--color-error)]"
            :info "text-[var(--color-info)]"
            "text-[var(--color-info)]")

          "text-[var(--color-info)]")

        default-icon
        (case color
          :primary lucide-icons/Info
          :success lucide-icons/CheckCircle
          :warning lucide-icons/AlertTriangle
          :error lucide-icons/AlertCircle
          :info lucide-icons/Info
          lucide-icons/Info)

        used-icon (or icon default-icon)

        all-classes (str base-classes " " variant-colors " " (when-not @visible "opacity-0 scale-95") " " class)

        hide-toast (fn [_]
                     (reset! visible false)
                     (when on-hide
                       (on-hide)))]

    ;; Set up auto-hide timer on mount
    (r/create-class
     {:display-name "Toast"

      :component-did-mount
      (fn []
        (when auto-hide
          (js/setTimeout hide-toast auto-hide)))

      :reagent-render
      (fn [_ & _]
        (if @visible
          [:div {:class all-classes}
           [:div {:class "p-4 flex gap-3"}

            ;; Icon
            [:div {:class icon-color}
             [:> used-icon {:size 20}]]

            ;; Content
            [:div {:class "flex-1"}
             (into [:div] children)]

            ;; Action or dismiss
            (if action
              action
              [:button {:class "flex-shrink-0 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:opacity-75"
                        :on-click hide-toast
                        :aria-label "Dismiss"}
               [:> lucide-icons/X {:size 16}]])]]
          [:div]))})))