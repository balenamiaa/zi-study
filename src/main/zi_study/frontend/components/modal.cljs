(ns zi-study.frontend.components.modal
  (:require
   [reagent.core :as r]
   [goog.object :as gobj]
   ["react-dom" :as react-dom]
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.utilities :refer [cx]]))

(def modal-impl
  (r/create-class
   {:display-name "modal-impl"
    :component-did-mount
    (fn [this]
      (let [props (r/props this)
            on-close (:on-close props)
            close-on-overlay-click (get props :close-on-overlay-click true)]
        (when (and on-close close-on-overlay-click)
          (let [handler (fn [e]
                          (when (and (= (.-key e) "Escape") on-close)
                            (on-close)))]
            (gobj/set this "escapeKeyHandler" handler)
            (.addEventListener js/window "keydown" handler)))))

    :component-will-unmount
    (fn [this]
      (let [handler (gobj/get this "escapeKeyHandler")]
        (when handler
          (.removeEventListener js/window "keydown" handler)
          (gobj/remove this "escapeKeyHandler"))))

    :reagent-render
    (fn [props-arg & actual-children]
      (let [{:keys [show? on-close title size close-on-overlay-click class content-class
                    show-close-button footer center-content blur-backdrop]
             :or {size :md
                  close-on-overlay-click true
                  show-close-button true
                  center-content false
                  blur-backdrop true}} props-arg

            size-classes (case size
                           :sm "max-w-sm"
                           :md "max-w-md"
                           :lg "max-w-lg"
                           :xl "max-w-xl"
                           :full "max-w-full w-full h-full m-0 rounded-none"
                           "max-w-md")

            overlay-classes (cx "fixed inset-0 z-50 flex items-center justify-center p-4"
                                "bg-black/40 dark:bg-black/60 transition-opacity duration-300"
                                (when blur-backdrop "backdrop-blur-sm")
                                (if show? "opacity-100" "opacity-0 pointer-events-none"))

            modal-classes (cx "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)]"
                              "rounded-xl shadow-xl w-full border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                              size-classes
                              "overflow-hidden"
                              "transform transition-all duration-300"
                              (if show? "scale-100 opacity-100" "scale-95 opacity-0")
                              class)

            content-alignment (if center-content "text-center" "")
            handle-close (fn [] (when on-close (on-close)))
            handle-overlay-click (fn [e]
                                   (when (and close-on-overlay-click
                                              (= (.-target e) (.-currentTarget e)))
                                     (handle-close)))]
        [:div {:class overlay-classes
               :on-click handle-overlay-click}
         [:div {:class modal-classes
                :on-click (fn [e] (.stopPropagation e))}
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
                                    "transition-colors duration-150 p-1 rounded-full"
                                    "hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)]")
                         :on-click handle-close
                         :aria-label "Close"}
                [:> lucide-icons/X {:size 18}]])])
          (when (and (not title) show-close-button)
            ^{:key "modal-close-button"}
            [:div {:class "absolute top-4 right-4 z-10"}
             [:button {:class (cx "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"
                                  "hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]"
                                  "transition-colors duration-150 p-1 rounded-full"
                                  "hover:bg-[var(--color-primary-50)] dark:hover:bg-[rgba(233,30,99,0.15)]")
                       :on-click handle-close
                       :aria-label "Close"}
              [:> lucide-icons/X {:size 18}]]])
          ^{:key "modal-content-wrapper"}
          (into [:div {:class (cx "p-6" content-alignment content-class)}] actual-children)
          (when footer
            ^{:key "modal-footer"}
            [:div {:class (cx "px-6 py-4 border-t flex justify-end gap-3"
                              "bg-gradient-to-r from-[var(--color-light-card)] to-[rgba(249,228,236,0.3)]"
                              "dark:from-[var(--color-dark-card)] dark:to-[rgba(46,31,45,0.6)]"
                              "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")}
             footer])]]))}))

(defn modal
  "A modal dialog component for displaying content over the page.
   Options are passed as a map, children follow.
   - show?: true/false - controls whether modal is visible (required in props)
   - on-close: function to call when closing modal
   - title: optional modal title string
   - etc. (see modal-impl props)"
  [& comp-args]
  (let [props (if (map? (first comp-args)) (first comp-args) {})
        children (if (map? (first comp-args)) (rest comp-args) comp-args)]
    (when (:show? props)
      (react-dom/createPortal
       (r/as-element (into [modal-impl props] children))
       js/document.body))))
