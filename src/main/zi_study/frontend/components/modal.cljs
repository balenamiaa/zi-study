(ns zi-study.frontend.components.modal
  (:require
   [reagent.core :as r]
   ["react-dom" :as react-dom]
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.utilities :refer [cx]]))

(defn modal
  "A modal dialog component for displaying content over the page.

   Options:
   - is-open: true/false - controls whether modal is visible (required)
   - on-close: function to call when closing modal
   - title: optional modal title string
   - size: :sm, :md, :lg, :xl, :full (default :md) - controls width of modal
   - close-on-overlay-click: true/false (default true) - close when clicking outside
   - class: additional CSS classes for modal container
   - content-class: additional CSS classes for modal content
   - show-close-button: true/false (default true) - show the close button
   - footer: optional content for the modal footer
   - center-content: true/false (default false) - center align content
   - blur-backdrop: true/false (default true) - apply blur effect to backdrop"
  []
  (fn [{:keys [is-open on-close title size close-on-overlay-click class content-class
               show-close-button footer center-content blur-backdrop]
        :or {size :md
             close-on-overlay-click true
             show-close-button true
             center-content false
             blur-backdrop true}}
       & children]
    (let [size-classes (case size
                         :sm "max-w-sm"
                         :md "max-w-md"
                         :lg "max-w-lg"
                         :xl "max-w-xl"
                         :full "max-w-full w-full h-full m-0 rounded-none"
                         "max-w-md")

          overlay-classes (cx "fixed inset-0 z-50 flex items-center justify-center p-4"
                              "bg-black/40 dark:bg-black/60 transition-all duration-300"
                              (when blur-backdrop "backdrop-blur-sm")
                              (if is-open "opacity-100" "opacity-0 pointer-events-none"))

          modal-classes (cx "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
                            "rounded-xl shadow-xl w-full border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                            size-classes
                            "overflow-hidden"
                            "transform transition-all duration-300"
                            (if is-open "scale-100 opacity-100" "scale-95 opacity-0")
                            class)

          content-alignment (if center-content "text-center" "")

          ;; Define handlers inside render function to access props
          handle-close (fn []
                         (when on-close
                           (on-close)))

          handle-overlay-click (fn [e]
                                 (when (and close-on-overlay-click
                                            (= (.-target e) (.-currentTarget e)))
                                   (handle-close)))]

      (when is-open
        (react-dom/createPortal
         (r/as-element
          [(r/create-class
            {:component-did-mount
             (fn [_]
               (when close-on-overlay-click
                 (.addEventListener
                  js/window "keydown"
                  (fn [e]
                    (when (and (= (.-key e) "Escape") on-close)
                      (on-close))))))

             :component-will-unmount
             (fn [_]
               (when close-on-overlay-click
                 (.removeEventListener
                  js/window "keydown"
                  (fn [e]
                    (when (and (= (.-key e) "Escape") on-close)
                      (on-close))))))

             :reagent-render
             (fn [_]
               [:div {:class overlay-classes
                      :on-click handle-overlay-click
                      :style {:pointer-events "auto"}}
                [:div {:class modal-classes
                       :on-click (fn [e] (.stopPropagation e))}

                 ;; Header (if title is provided)
                 (when title
                   ^{:key "modal-header"}
                   [:div {:class (cx "flex items-center justify-between px-6 py-4 border-b"
                                     "bg-gradient-to-r from-[var(--color-light-card)] to-[#F9E4EC]"
                                     "dark:from-[var(--color-dark-card)] dark:to-[rgba(46,31,45,0.9)]"
                                     "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")}
                    [:h3 {:class "text-lg font-medium text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
                     title]

                    (when show-close-button
                      [:button {:class (cx "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"
                                           "hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]"
                                           "transition-colors duration-200 p-1 rounded-full"
                                           "hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)]")
                                :on-click handle-close
                                :aria-label "Close"}
                       [:> lucide-icons/X {:size 18}]])])

                 ;; When no title but still want close button
                 (when (and (not title) show-close-button)
                   ^{:key "modal-close-button"}
                   [:div {:class "absolute top-4 right-4 z-10"}
                    [:button {:class (cx "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"
                                         "hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]"
                                         "transition-colors duration-200 p-1 rounded-full"
                                         "hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)]")
                              :on-click handle-close
                              :aria-label "Close"}
                     [:> lucide-icons/X {:size 18}]]])

                 ;; Content - wrap children in a div with a key and apply alignment
                 ^{:key "modal-content"}
                 [:div {:class (cx "p-6" content-alignment content-class)}
                  ;; Map unique keys to each child element if children is a sequence
                  (if (sequential? (first children))
                    (map-indexed
                     (fn [idx child]
                       (with-meta child {:key (str "modal-child-" idx)}))
                     children)
                    children)]

                 ;; Footer (if provided)
                 (when footer
                   ^{:key "modal-footer"}
                   [:div {:class (cx "px-6 py-4 border-t flex justify-end gap-3"
                                     "bg-gradient-to-r from-[var(--color-light-card)] to-[rgba(249,228,236,0.3)]"
                                     "dark:from-[var(--color-dark-card)] dark:to-[rgba(46,31,45,0.6)]"
                                     "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")}
                    footer])]])})])
         js/document.body)))))
