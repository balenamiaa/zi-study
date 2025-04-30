(ns zi-study.frontend.components.avatar-upload
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.components.badge :refer [avatar]]
   [zi-study.frontend.components.image-upload-modal :refer [image-upload-modal]]))

(defn avatar-upload
  "A component for uploading and managing avatar images.
   
   Options:
   - src: current avatar image URL
   - alt: alt text for image
   - initials: initials to show if no image is available
   - size: :xs, :sm, :md, :lg, :xl (default :md)
   - variant: :rounded, :circle (default :circle)
   - color: :primary, :secondary, :success, :warning, :error, :info (default :primary)
   - status: :online, :offline, :busy, :away - adds status indicator
   - disabled: true/false (default false) - disables upload functionality
   - on-image-selected: function to call when an image is selected (receives image data)
   - class: additional CSS classes"
  []
  (let [is-modal-open (r/atom false)]

    (fn [{:keys [src alt initials size variant color status disabled on-image-selected class]
          :or {disabled false}}]
      (let [handle-avatar-click (fn []
                                  (when-not disabled
                                    (reset! is-modal-open true)))

            handle-image-selected (fn [image-data]
                                    (when on-image-selected
                                      (on-image-selected image-data))
                                    (reset! is-modal-open false))

            handle-modal-close (fn []
                                 (reset! is-modal-open false))]

        [:div {:class "relative inline-block"}
         ;; Avatar component
         [:div {:class (str "cursor-pointer group " (when disabled "opacity-70"))
                :on-click handle-avatar-click}

          (if src
            ;; If we have an image, use the standard avatar
            [avatar {:src src
                     :alt alt
                     :initials initials
                     :size size
                     :variant variant
                     :color color
                     :status status
                     :class class}]

            ;; Custom empty avatar with better styling
            (let [size-val (or size :md)
                  variant-val (or variant :circle)

                  size-classes (case size-val
                                 :xs "w-6 h-6"
                                 :sm "w-8 h-8"
                                 :md "w-12 h-12"
                                 :lg "w-16 h-16"
                                 :xl "w-24 h-24"
                                 "w-12 h-12")

                  variant-classes (case variant-val
                                    :rounded "rounded-md"
                                    :circle "rounded-full"
                                    "rounded-full")

                  icon-size (case size-val
                              :xs 12
                              :sm 16
                              :md 20
                              :lg 24
                              :xl 32
                              20)]

              [:div {:class (str size-classes " " variant-classes " bg-gray-100 dark:bg-gray-800 border-2 border-dashed border-gray-300 dark:border-gray-600 flex items-center justify-center relative overflow-hidden")}

               ;; Skeleton animation background
               [:div {:class "absolute inset-0 bg-gradient-to-r from-gray-200 to-gray-300 dark:from-gray-700 dark:to-gray-800 animate-pulse"}]

               ;; Camera icon on top
               [:div {:class "relative z-10"}
                [:> lucide-icons/Camera {:size icon-size
                                         :class "text-gray-500 dark:text-gray-400"}]]]))

          ;; Overlay on hover
          [:div {:class "absolute inset-0 flex items-center justify-center bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none"}
           [:> lucide-icons/Camera {:size 24
                                    :class "text-white"}]]]

         ;; Upload modal - added key to fix React warning
         ^{:key "image-upload-modal"}
         [image-upload-modal
          {:is-open @is-modal-open
           :on-close handle-modal-close
           :on-image-selected handle-image-selected
           :initial-image-url src}]]))))
