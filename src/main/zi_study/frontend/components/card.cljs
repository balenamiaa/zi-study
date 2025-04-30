(ns zi-study.frontend.components.card
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]))

(defn card
  "A card component for displaying content in a contained, styled container.
   
   Options:
   - variant: :outlined, :elevated (default :elevated)
   - elevation: 1-5 (default 1) - shadow intensity for elevated cards
   - hover-effect: true/false (default false) - adds hover animation
   - full-width: true/false (default false) - makes the card take full width
   - class: additional CSS classes"
  [{:keys [variant elevation hover-effect full-width class]
    :or {variant :elevated
         elevation 1
         hover-effect false
         full-width false}}
   & children]

  (let [base-classes "rounded-lg overflow-hidden transition-all duration-300 ease-in-out dark:text-[var(--color-dark-text-primary)]"

        variant-classes
        (case variant
          :outlined "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
          :elevated
          (case elevation
            1 "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] shadow-sm"
            2 "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] shadow"
            3 "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] shadow-md"
            4 "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] shadow-lg"
            5 "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] shadow-xl"
            "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] shadow")
          "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] shadow-sm")

        hover-classes
        (when hover-effect
          (case variant
            :outlined "hover:border-[var(--color-primary)] dark:hover:border-[var(--color-primary-300)]"
            :elevated "hover:shadow-lg hover:translate-y-[-2px]"
            ""))

        width-classes (if full-width "w-full" "")

        all-classes (str base-classes " " variant-classes " " hover-classes " " width-classes " " class)]

    (into [:div {:class all-classes}] children)))

(defn card-header
  "Header component for cards, with title, subtitle, and optional icon/action.
   
   Options:
   - title: required header title
   - subtitle: optional subtitle text
   - icon: optional icon component
   - action: optional action element (e.g., a button)
   - class: additional CSS classes"
  [{:keys [title subtitle icon action class]}]

  [:div
   {:class (str "p-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] " class)}
   [:div {:class "flex items-center justify-between"}
    [:div {:class "flex items-center gap-3"}
     (when icon
       [:div {:class "text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"}
        [:> icon {:size 24}]])

     [:div
      [:h3 {:class "text-lg font-medium"} title]

      (when subtitle
        [:p {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
         subtitle])]]

    (when action
      [:div {:class "ml-4"} action])]])

(defn card-content
  "Content component for cards.
   
   Options:
   - no-padding: true/false (default false) - removes default padding
   - class: additional CSS classes"
  [{:keys [no-padding class] :or {no-padding false}} & children]

  (let [padding-class (if no-padding "" "p-4")
        class-name (str padding-class " " class)]

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

  (let [base-classes "px-4 py-3"

        border-class
        (if no-border
          ""
          "border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")

        align-class
        (case align
          :start "flex justify-start"
          :center "flex justify-center"
          :end "flex justify-end"
          :between "flex justify-between"
          "flex justify-end")

        all-classes (str base-classes " " border-class " " align-class " " class)]

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
   {:class (str "overflow-hidden "
                (when top "rounded-t-lg ")
                (when bottom "rounded-b-lg ")
                class)}
   [:img
    {:class (str "w-full object-cover " height)
     :src src
     :alt alt}]])