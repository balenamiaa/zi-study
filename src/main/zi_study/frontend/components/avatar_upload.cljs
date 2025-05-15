(ns zi-study.frontend.components.avatar-upload
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.components.image-upload-modal :refer [image-upload-modal]]
   [zi-study.frontend.utilities :refer [cx]]))

(defn avatar-upload
  "A component for uploading and displaying a profile avatar.
   
   Options:
   - current-url: Current avatar URL
   - on-change: Function called with new avatar URL when changed
   - size: :sm, :md, :lg (default :md)
   - class: Additional CSS classes"
  [{:keys [on-change size class]
    :or {size :md}}]

  (let [modal-open? (r/atom false)
        hover? (r/atom false)

        size-class (case size
                     :sm "w-16 h-16"
                     :md "w-24 h-24"
                     :lg "w-32 h-32"
                     "w-24 h-24")

        icon-size (case size
                    :sm 20
                    :md 28
                    :lg 36
                    28)]

    (fn [{:keys [current-url disabled]}]
      [:div {:class (cx "relative inline-block group" class)}
       ;; Avatar display
       [:div {:class (cx size-class "rounded-full overflow-hidden border-2 transition-all duration-300"
                         (if @hover?
                           "border-[var(--color-primary)] dark:border-[var(--color-primary-400)] shadow-md"
                           "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")
                         (when disabled "opacity-60 cursor-not-allowed"))}
        (if current-url
          [:img {:src current-url
                 :alt "Profile avatar"
                 :class (cx "w-full h-full object-cover transition-transform duration-300"
                            (when @hover? "scale-105"))}]
          [:div {:class (cx "w-full h-full flex items-center justify-center"
                            "bg-gradient-to-br from-[var(--color-light-bg-paper)] to-[var(--color-light-bg)] dark:from-[var(--color-dark-bg-paper)] dark:to-[var(--color-dark-bg)]")}
           [:> lucide-icons/User {:size icon-size
                                  :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]])]

       ;; Edit overlay that appears on hover
       (when-not disabled
         [:div {:class (cx "absolute inset-0 rounded-full flex items-center justify-center"
                           "bg-black/40 dark:bg-black/60 opacity-0 group-hover:opacity-100"
                           "transition-all duration-300 cursor-pointer"
                           "border-2 border-[var(--color-primary)] dark:border-[var(--color-primary-400)]")
                :on-click #(reset! modal-open? true)
                :on-mouse-enter #(reset! hover? true)
                :on-mouse-leave #(reset! hover? false)
                :role "button"
                :aria-label "Change profile picture"}
          [:div {:class "text-white text-xs font-medium tracking-wide flex flex-col items-center justify-center gap-1"}
           [:> lucide-icons/Camera {:size icon-size :className "mb-1"}]
           [:span "Change"]]])

       ;; Upload modal
       [image-upload-modal
        {:is-open @modal-open?
         :on-close #(reset! modal-open? false)
         :on-image-selected (fn [url]
                              (reset! modal-open? false)
                              (when on-change
                                (on-change url)))
         :initial-image-url current-url}]])))
