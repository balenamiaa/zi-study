(ns zi-study.frontend.components.card
  (:require
   [zi-study.frontend.utilities :refer [cx]]))

(defn card
  "A card component for displaying content in a contained, styled container.
   
   Options:
   - elevation: 1-5 (default 3) - shadow intensity for the card
   - hover-effect: true/false (default false) - adds hover animation
   - full-width: true/false (default false) - makes the card take full width
   - on-click: function - click handler for the card
   - class: additional CSS classes
   
   Any additional props will be passed to the underlying div"
  [{:keys [elevation hover-effect full-width class]
    :or {elevation 3
         hover-effect false
         full-width false}
    :as props}
   & children]

  (let [base-classes "rounded-lg overflow-hidden transition-all duration-300 ease-in-out dark:text-[var(--color-dark-text-primary)]"

        shadow-class
        (case elevation
          1 "shadow-1" 
          2 "shadow-2"
          3 "shadow-3"
          4 "shadow-4"
          5 "shadow-5"
          "shadow-3")
        
        card-classes (str "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] " 
                          shadow-class 
                          " border-2 border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")

        hover-classes
        (when hover-effect
          "hover:border-[var(--color-primary)] dark:hover:border-[var(--color-primary-300)] hover:shadow-4 hover:-translate-y-1")

        cursor-class (when (:on-click props) "cursor-pointer")
        width-classes (if full-width "w-full" "")
        all-classes (cx base-classes card-classes hover-classes width-classes cursor-class class)

        ;; Remove props we've already handled to avoid React warnings
        div-props (-> props
                      (dissoc :elevation :hover-effect :full-width :class)
                      (assoc :class all-classes))]

    (into [:div div-props] children)))

(defn card-header
  "Header component for cards, with title, subtitle, and optional icon/action.
   
   Options:
   - title: required header title
   - subtitle: optional subtitle text
   - icon: optional icon component
   - action: optional action element (e.g., a button)
   - class: additional CSS classes
   - accent-color: optional accent color (:primary, :secondary, :success, :warning, :info, :error)"
  [{:keys [title subtitle icon action class accent-color]
    :or {accent-color :primary}}]

  (let [accent-styles (case accent-color
                        :primary "border-l-4 border-l-[var(--color-primary)] dark:border-l-[var(--color-primary-300)]"
                        :secondary "border-l-4 border-l-[var(--color-secondary)] dark:border-l-[var(--color-secondary-light)]"
                        :success "border-l-4 border-l-[var(--color-success)] dark:border-l-[var(--color-success-300)]"
                        :warning "border-l-4 border-l-[var(--color-warning)] dark:border-l-[var(--color-warning-300)]"
                        :error "border-l-4 border-l-[var(--color-error)] dark:border-l-[var(--color-error-300)]"
                        :info "border-l-4 border-l-[var(--color-info)] dark:border-l-[var(--color-info-300)]"
                        "")]

    [:div
     {:class (cx "px-6 py-4 border-b bg-gradient-to-r from-[var(--color-light-card)] to-[#F9E4EC] "
                 "dark:from-[var(--color-dark-card)] dark:to-[rgba(46,31,45,0.9)] "
                 "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                 accent-styles
                 class)}
     [:div {:class "flex items-center justify-between"}
      [:div {:class "flex items-center gap-3"}
       (when icon
         [:div {:class "text-[var(--color-primary)] dark:text-[var(--color-primary-300)] p-2 rounded-full bg-[rgba(233,30,99,0.08)] dark:bg-[rgba(233,30,99,0.15)] select-none"}
          [:> icon {:size 24}]])

       [:div
        [:h3 {:class "text-xl font-medium tracking-tight"} title]

        (when subtitle
          [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mt-1 font-light"}
           subtitle])]]

      (when action
        [:div {:class "ml-4"} action])]]))

(defn card-content
  "Content component for cards.
   
   Options:
   - no-padding: true/false (default false) - removes default padding
   - class: additional CSS classes"
  [{:keys [no-padding class] :or {no-padding false}} & children]

  (let [padding-class (if no-padding "" "px-6 py-5")
        class-name (cx padding-class class)]
    (into [:div {:class class-name}] children)))

(defn card-footer
  "Footer component for cards, typically used for actions.
   
   Options:
   - align: :start, :center, :end, :between (default :end) - horizontal alignment
   - no-border: true/false (default false) - removes top border
   - class: additional CSS classes"
  [{:keys [align no-border class] :or {align :end
                                       no-border false}}
   & children]

  (let [base-classes "px-6 py-4"

        border-class
        (if no-border
          ""
          "border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")

        align-class
        (case align
          :start "flex justify-start items-center gap-2"
          :center "flex justify-center items-center gap-2"
          :end "flex justify-end items-center gap-2"
          :between "flex justify-between items-center"
          "flex justify-end items-center gap-2")

        all-classes (cx base-classes border-class align-class class)]

    (into [:div {:class all-classes}] children)))

(defn card-media
  "Media component for cards to display images.
   
   Options:
   - src: image source URL (required)
   - alt: image alt text (required)
   - height: CSS height value (default 'h-48')
   - top: true/false (default true) - rounds the top corners
   - bottom: true/false (default false) - rounds the bottom corners
   - class: additional CSS classes"
  [{:keys [src alt height top bottom class]
    :or {height "h-48"
         top true
         bottom false}}]

  [:div
   {:class (cx "overflow-hidden"
               (when top "rounded-t-lg")
               (when bottom "rounded-b-lg")
               class)}
   [:img
    {:class (cx "w-full object-cover" height)
     :src src
     :alt alt}]])