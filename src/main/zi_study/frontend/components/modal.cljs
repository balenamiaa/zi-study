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
   - show-close-button: true/false (default true) - show the close button"
  []
  (fn [{:keys [is-open on-close title size close-on-overlay-click class content-class show-close-button]
        :or {size :md
             close-on-overlay-click true
             show-close-button true}}
       & children]
    (let [size-classes (case size
                         :sm "max-w-sm"
                         :md "max-w-md"
                         :lg "max-w-lg"
                         :xl "max-w-xl"
                         :full "max-w-full w-full h-full m-0 rounded-none"
                         "max-w-md")

          overlay-classes (cx "fixed inset-0 z-50 flex items-center justify-center p-4"
                              "bg-black/50 transition-opacity duration-200"
                              (if is-open "opacity-100" "opacity-0 pointer-events-none"))

          modal-classes (cx "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
                            "rounded-lg shadow-lg w-full" 
                            size-classes 
                            "overflow-hidden"
                            "transform transition-all duration-200"
                            (if is-open "scale-100 opacity-100" "scale-95 opacity-0")
                            class)

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
         (r/as-element [:div {:class overlay-classes
                              :on-click handle-overlay-click
                              :style {:pointer-events "auto"}}
                        [:div {:class modal-classes
                               :on-click (fn [e] (.stopPropagation e))}

                         ;; Header (if title is provided)
                         (when title
                           ^{:key "modal-header"}
                           [:div {:class "flex items-center justify-between px-6 py-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
                            [:h3 {:class "text-lg font-medium text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
                             title]

                            (when show-close-button
                              [:button {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)] transition-colors duration-200"
                                        :on-click handle-close
                                        :aria-label "Close"}
                               [:> lucide-icons/X {:size 20}]])])

                         ;; When no title but still want close button
                         (when (and (not title) show-close-button)
                           ^{:key "modal-close-button"}
                           [:div {:class "absolute top-4 right-4"}
                            [:button {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)] transition-colors duration-200"
                                      :on-click handle-close
                                      :aria-label "Close"}
                             [:> lucide-icons/X {:size 20}]]])

                         ;; Content - wrap children in a div with a key
                         ^{:key "modal-content"}
                         [:div {:class (cx "p-6" content-class)}
                          ;; Map unique keys to each child element if children is a sequence
                          (if (sequential? (first children))
                            (map-indexed
                             (fn [idx child]
                               (with-meta child {:key (str "modal-child-" idx)}))
                             children)
                            children)]]])

         ;; Render into document body
         js/document.body)))))
