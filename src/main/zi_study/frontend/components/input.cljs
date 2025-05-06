(ns zi-study.frontend.components.input
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide]))

(def ^:private input-base-style
  (str "input w-full rounded transition-colors bg-transparent focus:outline-none"
       "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] "
       "disabled:opacity-50 disabled:cursor-not-allowed"))

(def ^:private label-base-style
  (str "block mb-1 text-xs font-medium transition-colors mx-2"
       "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"))

(def ^:private helper-base-style
  (str "mt-1 text-xs transition-colors "
       "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"))

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
                              :outlined (str "border "
                                             (if error-text
                                               "border-[var(--color-error)] focus:border-[var(--color-error)]"
                                               (str "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] "
                                                    "focus:border-[var(--color-primary)] dark:focus:border-[var(--color-primary-300)]")))
                              :filled (str (if error-text
                                             "bg-[var(--color-error-50)] focus:bg-[var(--color-error-50)]"
                                             (str "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] "
                                                  "focus:bg-[var(--color-light-bg)] dark:focus:bg-[var(--color-dark-bg)]"))))

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

              icon-wrapper-style (str "absolute top-0 bottom-0 flex items-center justify-center "
                                      (case size
                                        :sm "w-8"
                                        :md "w-10"
                                        :lg "w-12"))

              icon-style (str "text-[var(--color-light-text-secondary)] "
                              "dark:text-[var(--color-dark-text-secondary)]")

              active-icon-style (str "text-[var(--color-primary)] "
                                     "dark:text-[var(--color-primary-300)] "
                                     "cursor-pointer")

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

          [:div {:class (str width-style " " class)}

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
              [:div {:class (str icon-wrapper-style " left-0")}
               [:> start-icon {:size icon-size
                               :className icon-style}]])

            ;; End icon - debouncer, password toggle, or custom
            (when has-end-icon
              [:div {:class (str icon-wrapper-style " right-0 "
                                 (when (and (not @rx-is-debouncing) (or on-end-icon-click (= type :password)))
                                   "cursor-pointer"))
                     :on-click (cond
                                 @rx-is-debouncing nil
                                 (= type :password) toggle-password-visibility
                                 :else on-end-icon-click)}
               (cond
                 @rx-is-debouncing
                 [:> lucide/Loader2 {:size icon-size
                                     :className (str icon-style " animate-spin text-[var(--color-primary)]")}]

                 (= type :password)
                 (if @rx-show-password
                   [:> lucide/EyeOff {:size icon-size
                                      :className active-icon-style}]
                   [:> lucide/Eye {:size icon-size
                                   :className active-icon-style}])

                 end-icon
                 [:> end-icon {:size icon-size
                               :className (if on-end-icon-click
                                            active-icon-style
                                            icon-style)}]

                 :else nil)])

            ;; Input element
            [:input
             (merge
              sanitized-props
              {:id input-id
               :name (or name input-id)
               :type effective-type
               :value value
               :on-change handle-change
               :placeholder placeholder
               :disabled disabled
               :required required
               :class (str input-base-style " " variant-style " " size-style " "
                           start-padding " " end-padding
                           (when error-text " border-[var(--color-error)]"))
               :autoComplete autoComplete
               :maxLength maxLength})]]

           ;; Helper text or error message
           (cond
             error-text [:p {:class error-style} error-text]
             helper-text [:p {:class helper-base-style} helper-text])]))})))

;; Textarea Component
(defn textarea
  "A textarea component with various styling options.
   
   Options:
   - id: Optional id for the textarea element
   - name: Input name attribute
   - label: Label text
   - value: Textarea value
   - on-change: Change handler function
   - placeholder: Placeholder text
   - disabled: Whether textarea is disabled
   - required: Whether textarea is required
   - helper-text: Helper text displayed below textarea
   - error-text: Error text displayed below textarea (takes precedence over helper-text)
   - variant: :filled or :outlined styling
   - rows: Number of rows to display
   - resize: :both, :horizontal, :vertical, or :none
   - full-width: Whether textarea should take full width
   - class: Additional CSS classes
   - maxLength: Maximum input length"
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
           resize
           full-width
           class
           maxLength]
    :or   {variant    :filled
           rows       4
           resize     :vertical
           full-width true
           disabled   false
           required   false}
    :as   props}]

  (let [input-id (format-input-id id)

        variant-style (case variant
                        :outlined (str "border "
                                       (if error-text
                                         "border-[var(--color-error)] focus:border-[var(--color-error)]"
                                         (str "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] "
                                              "focus:border-[var(--color-primary)] dark:focus:border-[var(--color-primary-300)]")))
                        :filled (str (if error-text
                                       "bg-[var(--color-error-50)] focus:bg-[var(--color-error-50)]"
                                       (str "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] "
                                            "focus:bg-[var(--color-light-bg)] dark:focus:bg-[var(--color-dark-bg)]"))))

        size-style "px-4 py-2"

        width-style (if full-width "w-full" "w-auto")

        resize-style (case resize
                       :both "resize"
                       :horizontal "resize-x"
                       :vertical "resize-y"
                       :none "resize-none")

        ;; Remove props we've handled to avoid passing them to the textarea element
        sanitized-props (dissoc props
                                :variant :full-width
                                :helper-text :error-text
                                :resize :class)]

    [:div {:class (str width-style " " class)}

     ;; Label
     (when label
       [:label {:for input-id
                :class label-base-style}
        label
        (when required
          [:span.ml-1 {:class "text-[var(--color-error)]"} "*"])])

     ;; Textarea element
     [:textarea
      (merge
       sanitized-props
       {:id input-id
        :name (or name input-id)
        :value (or value "")
        :on-change #(when on-change
                      (on-change %))
        :placeholder placeholder
        :disabled disabled
        :required required
        :rows rows
        :class (str input-base-style " " variant-style " " size-style " " resize-style
                    (when error-text " border-[var(--color-error)]"))
        :maxLength maxLength})]

     ;; Helper text or error message
     (cond
       error-text [:p {:class error-style} error-text]
       helper-text [:p {:class helper-base-style} helper-text])]))