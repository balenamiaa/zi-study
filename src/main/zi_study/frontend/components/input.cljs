(ns zi-study.frontend.components.input
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide]
   [zi-study.frontend.utilities :refer [cx]]))

(def ^:private input-base-style
  "input w-full rounded transition-colors bg-transparent focus:outline-none text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] disabled:opacity-50 disabled:cursor-not-allowed")

(def ^:private label-base-style
  "block mb-1 text-xs font-medium transition-colors mx-2 text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]")

(def ^:private helper-base-style
  "mt-1 text-xs transition-colors text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]")

(def ^:private error-style
  "mt-1 text-xs text-[var(--color-error)]")

(defn- format-input-id [id]
  (if id
    id
    (str "input-" (random-uuid))))

;; Text Input Component (Form-2 for Debouncing)
(def text-input
  (let [rx-timer-id (r/atom nil)
        rx-show-password (r/atom false)
        rx-is-debouncing (r/atom false)]
    (r/create-class
     {:display-name "TextInput"

      :component-will-unmount
      (fn [_this]
        (when-let [timer-id @rx-timer-id]
          (js/clearTimeout timer-id))
        (reset! rx-timer-id nil)
        (reset! rx-show-password false)
        (reset! rx-is-debouncing false))

      :reagent-render
      (fn [props]
        (let [{:keys [id
                      name
                      type
                      label
                      value
                      on-change
                      on-change-debounced
                      placeholder
                      disabled
                      required
                      helper-text
                      error-text
                      start-icon
                      end-icon
                      on-end-icon-click
                      variant
                      size
                      full-width
                      class
                      autoComplete
                      maxLength]
               :or   {type       :text
                      variant    :filled
                      size       :md
                      full-width true
                      disabled   false
                      required   false}}
              props

              ;; Debounce settings
              debounce-time (:time on-change-debounced 300) ; Default 300ms
              debounced-callback (:callback on-change-debounced)

              input-id (format-input-id id)

              effective-type (if (and (= type :password) @rx-show-password)
                               :text
                               type)

              variant-style (case variant
                              :outlined (if error-text
                                          "border border-[var(--color-error)] focus:border-[var(--color-error)]"
                                          "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] focus:border-[var(--color-primary)] dark:focus:border-[var(--color-primary-300)]")
                              :filled (if error-text
                                        "bg-[var(--color-error-50)] focus:bg-[var(--color-error-50)]"
                                        "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] focus:bg-[var(--color-light-bg)] dark:focus:bg-[var(--color-dark-bg)]"))

              size-style (case size
                           :sm "px-3 py-1 text-sm"
                           :md "px-4 py-2 text-base"
                           :lg "px-5 py-3 text-lg")

              width-style (if full-width "w-full" "w-auto")

              icon-size (case size
                          :sm 16
                          :md 18
                          :lg 20)

              has-start-icon (boolean start-icon)
              has-end-icon (or @rx-is-debouncing (boolean end-icon) (= type :password))

              start-padding (if has-start-icon
                              (case size
                                :sm "pl-8"
                                :md "pl-10"
                                :lg "pl-12")
                              "")

              end-padding (if has-end-icon
                            (case size
                              :sm "pr-8"
                              :md "pr-10"
                              :lg "pr-12")
                            "")

              icon-wrapper-style (cx "absolute top-0 bottom-0 flex items-center justify-center"
                                     (case size
                                       :sm "w-8"
                                       :md "w-10"
                                       :lg "w-12"))

              icon-style "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"

              active-icon-style "text-[var(--color-primary)] dark:text-[var(--color-primary-300)] cursor-pointer"

              toggle-password-visibility (fn [e]
                                           (.preventDefault e)
                                           (.stopPropagation e)
                                           (swap! rx-show-password not))

              ;; Combined change handler (debounced or direct)
              handle-change (fn [event]
                              (when on-change (on-change event))
                              (when debounced-callback
                                (when-let [timer-id @rx-timer-id]
                                  (js/clearTimeout timer-id))
                                (reset! rx-is-debouncing true)
                                (reset! rx-timer-id
                                        (js/setTimeout
                                         (fn []
                                           (reset! rx-is-debouncing false)
                                           (debounced-callback event))
                                         debounce-time))))

              ;; Filter props passed to the actual input element
              sanitized-props (dissoc props
                                      :variant :size :full-width
                                      :helper-text :error-text
                                      :start-icon :end-icon :on-end-icon-click
                                      :class :on-change :on-change-debounced)]

          [:div {:class (cx width-style class)}

           ;; Label
           (when label
             [:label {:for input-id
                      :class label-base-style}
              label
              (when required
                [:span.ml-1 {:class "text-[var(--color-error)]"} "*"])])

           ;; Input container
           [:div.relative

            ;; Start icon
            (when has-start-icon
              [:div {:class (cx icon-wrapper-style "left-0")}
               [:> start-icon {:size icon-size
                               :className icon-style}]])

            ;; End icon - debouncer, password toggle, or custom
            (when has-end-icon
              [:div {:class (cx icon-wrapper-style "right-0"
                                (when (and (not @rx-is-debouncing) (or on-end-icon-click (= type :password)))
                                  "cursor-pointer"))
                     :on-click (cond
                                 @rx-is-debouncing nil
                                 (= type :password) toggle-password-visibility
                                 :else on-end-icon-click)}
               (cond
                 @rx-is-debouncing
                 [:> lucide/Loader2 {:size icon-size
                                     :className (cx icon-style "animate-spin text-[var(--color-primary)]")}]

                 (= type :password)
                 (if @rx-show-password
                   [:> lucide/EyeOff {:size icon-size
                                      :className active-icon-style}]
                   [:> lucide/Eye {:size icon-size
                                   :className active-icon-style}])

                 end-icon
                 [:> end-icon {:size icon-size
                               :className (if on-end-icon-click active-icon-style icon-style)}])])

            ;; Input element
            [:input
             (merge
              {:id input-id
               :name (or name input-id)
               :type effective-type
               :value value
               :placeholder placeholder
               :disabled disabled
               :required required
               :autoComplete autoComplete
               :maxLength maxLength
               :on-change handle-change
               :class (cx input-base-style
                          variant-style
                          size-style
                          start-padding
                          end-padding
                          (when error-text "border-[var(--color-error)] focus:border-[var(--color-error)]"))}
              sanitized-props)]]

           ;; Helper text or error
           (cond
             error-text
             [:div {:class error-style} error-text]

             helper-text
             [:div {:class helper-base-style} helper-text])]))})))

;; Textarea Component
(defn textarea
  "A textarea component for multi-line text input.
   
   Options:
   - id: input id
   - name: input name
   - label: label text
   - value: current value
   - on-change: function to call when value changes
   - placeholder: placeholder text
   - disabled: true/false (default false)
   - required: true/false (default false)
   - helper-text: additional information text
   - error-text: error message (adds error styling)
   - variant: :outlined, :filled (default :filled)
   - rows: number of rows (default 4)
   - max-rows: maximum number of rows for auto-resize
   - auto-resize: true/false - automatically resize based on content (default false)
   - class: additional CSS classes"
  [{:keys [id
           name
           label
           value
           on-change
           placeholder
           disabled
           required
           helper-text
           error-text
           variant
           rows
           max-rows
           auto-resize
           class]
    :or {variant :filled
         disabled false
         required false
         rows 4
         auto-resize false}}]

  (let [input-id (format-input-id id)
        textarea-ref (r/atom nil)

        variant-style (case variant
                        :outlined (if error-text
                                    "border border-[var(--color-error)] focus:border-[var(--color-error)]"
                                    "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] focus:border-[var(--color-primary)] dark:focus:border-[var(--color-primary-300)]")
                        :filled (if error-text
                                  "bg-[var(--color-error-50)] focus:bg-[var(--color-error-50)]"
                                  "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] focus:bg-[var(--color-light-bg)] dark:focus:bg-[var(--color-dark-bg)]"))

        adjust-height (fn []
                        (when (and auto-resize @textarea-ref)
                          (let [el @textarea-ref]
                            (set! (.-height (.-style el)) "auto")
                            (let [new-height (.-scrollHeight el)
                                  max-height (when max-rows
                                               (* max-rows (/ (.-scrollHeight el) (count (.split (.-value el) "\n")))))]
                              (set! (.-height (.-style el))
                                    (if (and max-height (> new-height max-height))
                                      (str max-height "px")
                                      (str new-height "px")))))))]

    (r/create-class
     {:component-did-mount
      (fn [_]
        (adjust-height))

      :component-did-update
      (fn [_ [_ old-props]]
        (when (not= (:value old-props) value)
          (adjust-height)))

      :reagent-render
      (fn []
        [:div {:class (cx "w-full" class)}
         ;; Label
         (when label
           [:label {:for input-id
                    :class label-base-style}
            label
            (when required
              [:span.ml-1 {:class "text-[var(--color-error)]"} "*"])])

         ;; Textarea
         [:textarea
          {:id input-id
           :name (or name input-id)
           :value value
           :placeholder placeholder
           :disabled disabled
           :required required
           :rows rows
           :on-change (fn [e]
                        (when on-change (on-change e))
                        (when auto-resize (adjust-height)))
           :ref #(reset! textarea-ref %)
           :class (cx input-base-style
                      variant-style
                      "px-4 py-2 resize-none"
                      (when error-text "border-[var(--color-error)] focus:border-[var(--color-error)]"))}]

         ;; Helper text or error
         (cond
           error-text
           [:div {:class error-style} error-text]

           helper-text
           [:div {:class helper-base-style} helper-text])])})))