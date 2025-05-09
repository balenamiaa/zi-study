(ns zi-study.frontend.components.skeleton
  (:require [zi-study.frontend.utilities :refer [cx]]))

(defn skeleton
  "A skeleton loading component with multiple variants.
   
   Options:
   - variant: :text, :circular, :rectangular, :avatar, :card (default: :rectangular)
   - width: Width of skeleton (default: 100%)
   - height: Height of skeleton (default: based on variant)
   - animation: :pulse, :wave, or nil for no animation (default: :pulse)
   - class: Additional CSS classes"
  [{:keys [variant width height animation class]
    :or {variant :rectangular
         animation :pulse}}]

  (let [base-classes "bg-gradient-to-r from-[var(--color-light-divider)] to-[var(--color-light-bg-paper)] dark:from-[var(--color-dark-divider)] dark:to-[var(--color-dark-bg-paper)]"
        animation-class (case animation
                          :pulse "animate-pulse"
                          :wave "animate-skeleton-wave"
                          nil)

        variant-classes (case variant
                          :text "h-4 rounded-md overflow-hidden"
                          :circular "rounded-full overflow-hidden"
                          :avatar "rounded-full overflow-hidden border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                          :card "rounded-xl overflow-hidden border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] shadow-sm"
                          :rectangular "rounded-md overflow-hidden")

        default-height (case variant
                         :text "1rem"
                         :circular "3rem"
                         :avatar "3rem"
                         :card "12rem"
                         :rectangular "1.5rem")

        default-width (case variant
                        :text "100%"
                        :circular "3rem"
                        :avatar "3rem"
                        :card "100%"
                        :rectangular "100%")]

    [:div {:class (cx base-classes variant-classes animation-class class)
           :style {:width (or width default-width)
                   :height (or height default-height)}}]))

(defn skeleton-text
  "Skeleton for text lines with configurable rows and width variation.
   
   Options:
   - rows: Number of text rows (default: 3)
   - variant-width: Randomize width of each row (default: true)
   - animation: :pulse, :wave or nil (default: :pulse)
   - class: Additional CSS classes"
  [{:keys [rows variant-width animation class]
    :or {rows 3
         variant-width true
         animation :pulse}}]

  [:div {:class class}
   (for [i (range rows)]
     ^{:key i}
     [skeleton {:variant :text
                :animation animation
                :width (when variant-width
                         (str (- 100 (* i (if (even? i) 15 10))) "%"))
                :class "mb-2"}])])

(defn skeleton-card
  "Skeleton for a card with optional elements.
   
   Options:
   - header: Include header (default: true)
   - media: Include media/image area (default: true)
   - content-rows: Number of text rows (default: 3)
   - footer: Include footer (default: true)
   - animation: :pulse, :wave or nil (default: :pulse)
   - class: Additional CSS classes"
  [{:keys [header media content-rows footer animation class]
    :or {header true
         media true
         content-rows 3
         footer true
         animation :pulse}}]

  [:div {:class (cx "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] rounded-xl overflow-hidden shadow-sm"
                    "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                    class)}

   ;; Media
   (when media
     [skeleton {:variant :rectangular
                :height "12rem"
                :animation animation}])

   ;; Header
   (when header
     [:div {:class "px-6 py-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
      [skeleton {:variant :text
                 :width "60%"
                 :height "1.5rem"
                 :animation animation
                 :class "mb-2"}]
      [skeleton {:variant :text
                 :width "40%"
                 :height "1rem"
                 :animation animation}]])

   ;; Content
   [:div {:class "px-6 py-4"}
    [skeleton-text {:rows content-rows
                    :animation animation}]]

   ;; Footer
   (when footer
     [:div {:class "px-6 py-4 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] flex gap-3 justify-end"}
      [skeleton {:variant :rectangular
                 :width "5rem"
                 :height "2rem"
                 :animation animation
                 :class "rounded-md"}]
      [skeleton {:variant :rectangular
                 :width "5rem"
                 :height "2rem"
                 :animation animation
                 :class "rounded-md"}]])])

(defn skeleton-avatar-with-text
  "Skeleton for avatar with text lines (like a comment or user card).
   
   Options:
   - rows: Number of text rows (default: 2)
   - avatar-size: Size of avatar (default: 3rem)
   - animation: :pulse, :wave or nil (default: :pulse)
   - class: Additional CSS classes"
  [{:keys [rows avatar-size animation class]
    :or {rows 2
         avatar-size "3rem"
         animation :pulse}}]

  [:div {:class (cx "flex gap-3 p-3 rounded-lg hover:bg-[var(--color-light-bg-paper)] dark:hover:bg-[var(--color-dark-bg-paper)] transition-colors" class)}
   [skeleton {:variant :avatar
              :width avatar-size
              :height avatar-size
              :animation animation}]

   [:div {:class "flex-1"}
    (for [i (range rows)]
      ^{:key i}
      [skeleton {:variant :text
                 :width (if (zero? i) "40%" "70%")
                 :animation animation
                 :class "mb-2"}])]])

(defn skeleton-table
  "Skeleton for a table with rows and columns.
   
   Options:
   - rows: Number of rows (default: 4)
   - cols: Number of columns (default: 3)
   - header: Include header row (default: true)
   - animation: :pulse, :wave or nil (default: :pulse)
   - class: Additional CSS classes"
  [{:keys [rows cols header animation class]
    :or {rows 4
         cols 3
         header true
         animation :pulse}}]

  [:div {:class (cx "w-full rounded-lg overflow-hidden border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]" class)}
   (when header
     [:div {:class (cx "flex gap-4 p-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                       "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]")}
      (for [i (range cols)]
        ^{:key i}
        [skeleton {:variant :text
                   :width (str (+ 20 (* i 5)) "%")
                   :height "1.5rem"
                   :animation animation}])])

   [:div {:class "p-2"}
    (for [r (range rows)]
      ^{:key r}
      [:div {:class (cx "flex gap-4 p-2 rounded-md"
                        (when (even? r) "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"))}
       (for [c (range cols)]
         ^{:key c}
         [skeleton {:variant :text
                    :width (str (+ 20 (* c 5)) "%")
                    :animation animation}])])]])

;; Add a global animation style for skeleton wave effect with improved gradient
(def ^:private wave-style
  (let [style-el (js/document.createElement "style")]
    (set! (.-textContent style-el) "
      @keyframes skeletonWave {
        0% { background-position: -200px 0; }
        100% { background-position: calc(200px + 100%) 0; }
      }
      .animate-skeleton-wave {
        background-size: 200px 100%;
        background-repeat: no-repeat;
        animation: skeletonWave 1.5s ease-in-out infinite;
        background-image: linear-gradient(
          90deg,
          transparent,
          rgba(var(--color-primary-rgb), 0.15), 
          transparent
        );
      }
      
      .dark .animate-skeleton-wave {
        background-image: linear-gradient(
          90deg,
          transparent,
          rgba(var(--color-primary-rgb), 0.2),
          transparent
        );
      }
    ")
    (.appendChild (.-head js/document) style-el)))

;; Initialize the style on namespace load
(defonce _ wave-style)