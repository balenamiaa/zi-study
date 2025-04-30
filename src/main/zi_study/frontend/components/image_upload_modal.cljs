(ns zi-study.frontend.components.image-upload-modal
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [goog.object :as gobj]
   [zi-study.frontend.components.button :refer [button]]
   [zi-study.frontend.components.modal :refer [modal]]
   [zi-study.frontend.components.alert :refer [alert]]
   [zi-study.frontend.components.input :refer [text-input]]))

(defn image-upload-modal
  "A modal for uploading images with multiple methods.
   
   Options:
   - is-open: true/false - controls visibility
   - on-close: function to call when modal is closed
   - on-image-selected: function to call with selected image data (now receives URL string)
   - initial-image-url: optional initial image URL"
  [{:keys [on-close on-image-selected initial-image-url]}]
  (let [active-method (r/atom :file-system)
        image-url (r/atom "")
        image-preview (r/atom initial-image-url)
        drag-active (r/atom false)
        file-input-ref (r/atom nil)
        upload-loading? (r/atom false)
        upload-error (r/atom nil)

        upload-image-data (fn [file-or-blob filename]
                            (reset! upload-error nil)
                            (reset! upload-loading? true)
                            (let [form-data (js/FormData.)]
                              (.append form-data "profile_picture" file-or-blob filename)
                              (-> (js/fetch "/api/uploads/profile-picture"
                                            (clj->js {:method "POST"
                                                      :body form-data}))
                                  (.then (fn [response]
                                           (if (.-ok response)
                                             (.json response)
                                             (js/Promise.reject "Upload failed"))))
                                  (.then (fn [data]
                                           (reset! upload-loading? false)
                                           (when-let [uploaded-url (gobj/get data "url")]
                                             (reset! image-preview uploaded-url)
                                             (when on-image-selected
                                               (on-image-selected uploaded-url)))))
                                  (.catch (fn [err]
                                            (reset! upload-loading? false)
                                            (let [error-msg (if (string? err)
                                                              err
                                                              (or (.. err -message) "An unknown upload error occurred."))]
                                              (reset! upload-error (str "Upload failed: " error-msg)))
                                            (js/console.error "Upload error:" err))))))

        handle-file-change (fn [e]
                             (let [file (-> e .-target .-files (aget 0))]
                               (when file
                                 ;; Create a temporary preview URL
                                 (let [url (.createObjectURL js/URL file)]
                                   (reset! image-preview url))
                                 ;; Trigger upload
                                 (upload-image-data file (:name file)))))

        handle-url-change (fn [url]
                            (reset! image-url url)
                            (if (or (nil? url) (empty? url))
                              (reset! image-preview nil)
                              (reset! image-preview url)))

        handle-url-submit (fn []
                            (when-not (or (nil? @image-url) (empty? @image-url))
                              (reset! upload-error nil)
                              (reset! upload-loading? true)
                              ;; Fetch the image from URL, convert to blob, then upload
                              (-> (js/fetch @image-url)
                                  (.then #(.blob %))
                                  (.then (fn [blob]
                                           (let [filename (or (second (re-find #"([^/]+)$" @image-url)) "image.jpg")]
                                             (upload-image-data blob filename))))
                                  (.catch (fn [err]
                                            (reset! upload-loading? false)
                                            (reset! upload-error "Failed to fetch image from URL.")
                                            (js/console.error "URL fetch error:" err))))))

        handle-paste (fn [e]
                       (let [items (.. e -clipboardData -items)]
                         (doseq [i (range (.-length items))]
                           (let [item (.item items i)]
                             (when (and item (.startsWith (.-type item) "image"))
                               (let [file (.getAsFile item)
                                     url (.createObjectURL js/URL file)]
                                 (reset! image-preview url)
                                 (upload-image-data file "pasted_image.png")))))))

        handle-drag (fn [e active]
                      (.preventDefault e)
                      (.stopPropagation e)
                      (reset! drag-active active))

        handle-drop (fn [e]
                      (.preventDefault e)
                      (.stopPropagation e)
                      (reset! drag-active false)
                      (let [file (-> e .-dataTransfer .-files (aget 0))]
                        (when file
                          (let [url (.createObjectURL js/URL file)]
                            (reset! image-preview url)
                            (upload-image-data file (:name file))))))

        open-file-dialog (fn []
                           (when (and @file-input-ref (not @upload-loading?))
                             (.click @file-input-ref)))]

    ;; Reset state when modal opens/closes
    (r/create-class
     {:component-did-update
      (fn [this [_ old-props]]
        (let [[new-props _] (r/argv this)]
          (when (and (not (:is-open old-props)) (:is-open new-props))
            (reset! image-preview (:initial-image-url new-props))
            (reset! image-url "")
            (reset! upload-error nil)
            (reset! upload-loading? false))))

      :reagent-render
      (fn [{:keys [is-open]}]
        [modal {:is-open is-open
                :on-close on-close
                :title "Upload Profile Image"
                :size :md
                :close-on-overlay-click (not @upload-loading?)
                :show-close-button (not @upload-loading?)}

         ;; Upload Error message
         (when @upload-error
           ^{:key "upload-error-alert"}
           [alert {:variant :soft
                   :color :error
                   :dismissible true
                   :on-dismiss #(reset! upload-error nil)
                   :class "mb-4"}
            @upload-error])

         ;; Image preview & Loading indicator
         ^{:key "image-preview-container"}
         [:div {:class "mb-6 flex justify-center"}
          [:div {:class "relative w-32 h-32"}
           (if @image-preview
             ^{:key "image-preview-with-image"}
             [:img {:class "w-32 h-32 rounded-full object-cover border-2"
                    :src @image-preview
                    :alt "Profile preview"}]
             ^{:key "image-preview-empty"}
             [:div {:class "w-32 h-32 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center relative overflow-hidden"}
              [:> lucide-icons/Image
               {:size 40
                :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]])

           ;; Loading overlay
           (when @upload-loading?
             ^{:key "loading-overlay"}
             [:div {:class "absolute inset-0 flex items-center justify-center bg-black/60 rounded-full"}
              [:div {:class "animate-spin rounded-full h-8 w-8 border-b-2 border-white"}]])]]

         ;; Upload method tabs
         ^{:key "upload-method-tabs"}
         [:div {:class "flex mb-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}

          ^{:key "tab-file-system"}
          [:button {:class (str "flex items-center px-4 py-2 text-sm font-medium border-b-2 transition-colors "
                                (if (= @active-method :file-system)
                                  "border-[var(--color-primary)] text-[var(--color-primary)] dark:text-[var(--color-primary-300)] dark:border-[var(--color-primary-300)]"
                                  "border-transparent text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]")
                                (when @upload-loading? " opacity-50 cursor-not-allowed"))
                    :on-click #(reset! active-method :file-system)
                    :disabled @upload-loading?}
           [:> lucide-icons/Upload {:size 16
                                    :className "mr-2"}]
           "Upload"]

          ^{:key "tab-url"}
          [:button {:class (str "flex items-center px-4 py-2 text-sm font-medium border-b-2 transition-colors "
                                (if (= @active-method :url)
                                  "border-[var(--color-primary)] text-[var(--color-primary)] dark:text-[var(--color-primary-300)] dark:border-[var(--color-primary-300)]"
                                  "border-transparent text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]")
                                (when @upload-loading? " opacity-50 cursor-not-allowed"))
                    :on-click #(reset! active-method :url)
                    :disabled @upload-loading?}
           [:> lucide-icons/Link {:size 16 :className "mr-2"}]
           "URL"]

          ^{:key "tab-paste"}
          [:button {:class (str "flex items-center px-4 py-2 text-sm font-medium border-b-2 transition-colors "
                                (if (= @active-method :paste)
                                  "border-[var(--color-primary)] text-[var(--color-primary)] dark:text-[var(--color-primary-300)] dark:border-[var(--color-primary-300)]"
                                  "border-transparent text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] hover:text-[var(--color-primary)] dark:hover:text-[var(--color-primary-300)]")
                                (when @upload-loading? " opacity-50 cursor-not-allowed"))
                    :on-click #(reset! active-method :paste)
                    :disabled @upload-loading?}
           [:> lucide-icons/Clipboard {:size 16 :className "mr-2"}]
           "Paste"]]

         ;; Tab content based on active method
         [:div {:key (str "tab-content-" (name @active-method))}
           (case @active-method
             :file-system
             [:div {:class "mt-4"}
              [:div
               {:class (str "border-2 border-dashed rounded-lg p-6 text-center transition-colors "
                            (if @drag-active
                              "border-[var(--color-primary)] bg-[var(--color-primary-50)] dark:bg-[rgba(240,98,146,0.1)]"
                              "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]")
                            (when @upload-loading? " opacity-50 cursor-not-allowed"))
                :on-drag-enter #(handle-drag % true)
                :on-drag-over #(handle-drag % true)
                :on-drag-leave #(handle-drag % false)
                :on-drop handle-drop
                :on-click (when-not @upload-loading?
                            (fn [e]
                              (.preventDefault e)
                              (open-file-dialog)))
                :role "button"
                :tab-index (when @upload-loading? -1)
                :aria-disabled @upload-loading?
                :aria-label "Upload image from file system"}
               [:> lucide-icons/Upload
                {:size 32
                 :className "mx-auto mb-2 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]
               [:p {:class "mb-1"} "Drag and drop your image here:"]
               [:p {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
                "or click to browse files"]
               [:input
                {:type "file"
                 :accept "image/*"
                 :class "hidden"
                 :ref #(when % (reset! file-input-ref %))
                 :on-change handle-file-change
                 :required false
                 :aria-hidden true
                 :id "file-upload-input"
                 :name "file-upload"
                 :disabled @upload-loading?}]]]

             :url
             [:div {:class "mt-4"}
              [text-input
               {:type :text
                :variant :outlined
                :label "Image URL"
                :placeholder "https://example.com/image.jpg"
                :value @image-url
                :on-change #(handle-url-change (.. % -target -value))
                :helper-text "Enter the URL of an image"
                :start-icon lucide-icons/Link
                :required false
                :full-width true
                :disabled @upload-loading?
                :class "mb-4"}]

              [button
               {:variant :primary
                :size :md
                :full-width true
                :start-icon lucide-icons/Check
                :disabled (or @upload-loading? (nil? @image-url) (empty? @image-url))
                :loading (when @upload-loading? true)
                :on-click (fn [_] (handle-url-submit))}
               (if @upload-loading? "Uploading..." "Use this image")]]

             :paste
             [:div {:class "mt-4"}
              [:div
               {:class (str "border-2 border-dashed rounded-lg p-6 text-center transition-colors border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                            (when @upload-loading? " opacity-50 cursor-not-allowed"))
                :tab-index (if @upload-loading? -1 0)
                :on-paste handle-paste
                :on-click (when-not @upload-loading?
                            (fn [e]
                              (.preventDefault e)
                              (.. e -currentTarget -focus)))
                :role "button"
                :aria-disabled @upload-loading?
                :aria-label "Paste image from clipboard"}

               ;; Hidden input to prevent validation
               [:input
                {:type "text"
                 :class "hidden"
                 :required false
                 :default-value ""
                 :aria-hidden true}]

               [:> lucide-icons/Clipboard
                {:size 32
                 :className "mx-auto mb-2 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]
               [:p {:class "mb-1"} "Click here and paste your image (Ctrl+V)"]
               [:p {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
                "You can paste from clipboard or screenshot"]]])]])})))
