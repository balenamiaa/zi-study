(ns zi-study.frontend.pages.components
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [zi-study.frontend.components.button :refer [button button-group]]
   [zi-study.frontend.components.card :refer [card card-header card-content card-footer card-media]]
   [zi-study.frontend.components.input :refer [text-input textarea]]
   [zi-study.frontend.components.badge :refer [badge avatar]]
   [zi-study.frontend.components.alert :refer [alert toast]]
   [zi-study.frontend.components.toggle :refer [toggle checkbox radio]]
   [zi-study.frontend.components.skeleton :refer [skeleton skeleton-text skeleton-card skeleton-avatar-with-text skeleton-table]]
   [zi-study.frontend.components.dropdown :refer [dropdown menu-item menu-divider menu-label]]))

(defn section-title [title]
  [:h2.text-2xl.font-medium.mb-4.mt-8
   {:class "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"
    :id (-> title str .toLowerCase (.replace #"\s+" "-"))}
   title])

(defn section-subtitle [title]
  [:h3.text-xl.font-medium.mb-3.mt-6
   {:class "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
   title])

(defn demo-row [& children]
  (into [:div.flex.flex-wrap.gap-4.items-center.mb-6] children))

(defn buttons-section []
  [:div
   [section-title "Buttons"]

   [section-subtitle "Button Variants"]
   [demo-row
    [button {:variant :primary} "Primary"]
    [button {:variant :secondary} "Secondary"]
    [button {:variant :outlined} "Outlined"]
    [button {:variant :text} "Text"]]

   [section-subtitle "Button Sizes"]
   [demo-row
    [button {:size :xs} "Extra Small"]
    [button {:size :sm} "Small"]
    [button {:size :md} "Medium"]
    [button {:size :lg} "Large"]
    [button {:size :xl} "Extra Large"]]

   [section-subtitle "Buttons with Icons"]
   [demo-row
    [button {:start-icon lucide-icons/Plus} "Add Item"]
    [button {:end-icon lucide-icons/ArrowRight} "Next"]
    [button {:variant :outlined
             :start-icon lucide-icons/Download} "Download"]
    [button {:variant :text
             :start-icon lucide-icons/Trash2
             :class "text-[var(--color-error)]"} "Delete"]]

   [section-subtitle "Button States"]
   [demo-row
    [button "Normal"]
    [button {:disabled true} "Disabled"]
    [button {:variant :outlined :disabled true} "Disabled"]]

   [section-subtitle "Button Groups"]
   [demo-row
    [button-group {}
     [button {:variant :outlined} "Left"]
     [button {:variant :outlined} "Middle"]
     [button {:variant :outlined} "Right"]]

    [button-group {:variant :vertical :class "ml-4"}
     [button {:variant :outlined :size :sm} "Top"]
     [button {:variant :outlined :size :sm} "Middle"]
     [button {:variant :outlined :size :sm} "Bottom"]]]])

(defn cards-section []
  (let [favorite (r/atom false)]
    (fn []
      [:div
       [section-title "Cards"]

       [section-subtitle "Basic Cards"]
       [demo-row
        [card {:class "max-w-sm"}
         [card-content {}
          [:p "A simple card with just content."]]]

        [card {:variant :outlined :class "max-w-sm"}
         [card-content {}
          [:p "An outlined card variant."]]]]

       [section-subtitle "Card with Header and Footer"]
       [card {:class "max-w-md mb-6 md:w-1/2"}
        [card-header {:title "Card Title"
                      :subtitle "Card subtitle"
                      :icon lucide-icons/Bookmark}]
        [card-content {}
         [:p.mb-4 "This is a card with a header and footer. Cards can contain various content types including text, images, and interactive elements."]
         [:p "They're perfect for organizing related content and actions."]]
        [card-footer {}
         [button {:variant :text :size :sm} "Cancel"]
         [button {:variant :primary :size :sm :class "ml-2"} "Submit"]]]

       [section-subtitle "Card with Media"]
       [card {:hover-effect true :class "max-w-sm mb-6"}
        [card-media {:src "https://images.unsplash.com/photo-1615529328331-f8917597711f?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=880&q=80"
                     :alt "Sample image"}]
        [card-header {:title "Beautiful Places"
                      :subtitle "Discover amazing locations"
                      :action [:div {:class "cursor-pointer text-[var(--color-primary)] dark:text-[var(--color-primary-300)]"
                                     :on-click #(swap! favorite not)}
                               [:> (if @favorite lucide-icons/Heart lucide-icons/HeartCrack) {:size 20}]]}]
        [card-content {}
         [:p "Explore beautiful destinations around the world. Each place offers unique experiences and unforgettable memories."]]
        [card-footer {}
         [button {:variant :outlined :size :sm
                  :start-icon lucide-icons/Share2} "Share"]
         [button {:variant :primary :size :sm
                  :start-icon lucide-icons/MapPin
                  :class "ml-2"} "Explore"]]]])))

(defn inputs-section []
  (let [input-value (r/atom "")
        textarea-value (r/atom "")
        password-value (r/atom "")
        error-value (r/atom "")]
    (fn []
      [:div
       [section-title "Inputs"]

       [section-subtitle "Text Inputs"]
       [:div.grid.grid-cols-1.md:grid-cols-2.gap-6.mb-6
        [text-input {:label "Standard Input"
                     :placeholder "Enter text here"
                     :value @input-value
                     :on-change #(reset! input-value (.. % -target -value))
                     :full-width true}]

        [text-input {:variant :filled
                     :label "Filled Input"
                     :placeholder "Enter text here"
                     :value @input-value
                     :on-change #(reset! input-value (.. % -target -value))
                     :full-width true}]]

       [section-subtitle "Input with Icons"]
       [:div.grid.grid-cols-1.md:grid-cols-2.gap-6.mb-6
        [text-input {:label "Input with Start Icon"
                     :start-icon lucide-icons/Search
                     :placeholder "Search..."
                     :full-width true}]

        [text-input {:label "Input with End Icon"
                     :end-icon lucide-icons/Calendar
                     :placeholder "Choose date..."
                     :full-width true}]]

       [section-subtitle "Password Input"]
       [:div.grid.grid-cols-1.md:grid-cols-2.gap-6.mb-6
        [text-input {:type :password
                     :label "Password"
                     :placeholder "Enter password"
                     :value @password-value
                     :on-change #(reset! password-value (.. % -target -value))
                     :helper-text "Your password must be at least 8 characters long"
                     :full-width true}]]

       [section-subtitle "Input with Error"]
       [:div.grid.grid-cols-1.md:grid-cols-2.gap-6.mb-6
        [text-input {:label "Email"
                     :type :email
                     :placeholder "Enter email"
                     :value @error-value
                     :on-change #(reset! error-value (.. % -target -value))
                     :error {:error true
                             :message "Please enter a valid email address"}
                     :full-width true}]]

       [section-subtitle "Textarea"]
       [:div.mb-6
        [textarea {:label "Message"
                   :placeholder "Enter your message"
                   :rows 4
                   :value @textarea-value
                   :on-change #(reset! textarea-value (.. % -target -value))
                   :full-width true}]]])))

(defn badges-section []
  [:div
   [section-title "Badges & Avatars"]

   [section-subtitle "Badge Variants"]
   [demo-row
    [:div.relative.inline-block.mr-6
     [button "Notifications"]
     [badge {:count 5
             :color :primary
             :position :top-right}]]

    [:div.relative.inline-block.mr-6
     [button {:variant :outlined} "Messages"]
     [badge {:count 12
             :max 9
             :color :error
             :position :top-right}]]

    [:div.relative.inline-block.mr-6
     [button {:variant :text
              :start-icon lucide-icons/Bell} "Updates"]
     [badge {:dot true
             :color :success
             :position :top-right}]]]

   [section-subtitle "Badge Colors"]
   [demo-row
    [badge {:color :primary} "Primary"]
    [badge {:color :secondary} "Secondary"]
    [badge {:color :success} "Success"]
    [badge {:color :warning} "Warning"]
    [badge {:color :error} "Error"]
    [badge {:color :info} "Info"]]

   [section-subtitle "Outlined Badges"]
   [demo-row
    [badge {:variant :outlined :color :primary} "Primary"]
    [badge {:variant :outlined :color :secondary} "Secondary"]
    [badge {:variant :outlined :color :success} "Success"]
    [badge {:variant :outlined :color :warning} "Warning"]
    [badge {:variant :outlined :color :error} "Error"]
    [badge {:variant :outlined :color :info} "Info"]]

   [section-subtitle "Avatars"]
   [demo-row
    [avatar {:size :xs}]
    [avatar {:size :sm}]
    [avatar {:size :md}]
    [avatar {:size :lg}]
    [avatar {:size :xl}]]

   [section-subtitle "Avatar with Image"]
   [demo-row
    [avatar {:src "https://images.unsplash.com/photo-1534528741775-53994a69daeb?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=764&q=80"
             :alt "User avatar"
             :size :lg}]

    [avatar {:src "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=687&q=80"
             :alt "User avatar"
             :size :lg}]]

   [section-subtitle "Avatar with Initials"]
   [demo-row
    [avatar {:initials "JD"
             :color :primary
             :size :md}]

    [avatar {:initials "MC"
             :color :secondary
             :size :md}]

    [avatar {:initials "TS"
             :color :success
             :size :md}]

    [avatar {:initials "AL"
             :color :warning
             :size :md}]

    [avatar {:initials "RW"
             :color :error
             :size :md}]]

   [section-subtitle "Avatar with Status"]
   [demo-row
    [avatar {:initials "JD"
             :status :online
             :size :md}]

    [avatar {:initials "MC"
             :status :offline
             :size :md}]

    [avatar {:initials "TS"
             :status :busy
             :size :md}]

    [avatar {:initials "AL"
             :status :away
             :size :md}]]])

(defn alerts-section []
  (let [dismissible-alert (r/atom true)
        show-success-toast (r/atom false)
        show-error-toast (r/atom false)]
    (fn []
      [:div
       [section-title "Alerts & Notifications"]

       [section-subtitle "Alert Variants"]
       (when @dismissible-alert
         [alert {:variant :soft
                 :color :info
                 :title "Information"
                 :dismissible true
                 :on-dismiss #(reset! dismissible-alert false)
                 :class "mb-4"}
          "This is an informational alert. You can dismiss it by clicking the X."])

       [alert {:variant :filled
               :color :success
               :title "Success"
               :class "mb-4"}
        "Operation completed successfully!"]

       [alert {:variant :outlined
               :color :warning
               :title "Warning"
               :class "mb-4"}
        "Please be careful with this action."]

       [alert {:variant :soft
               :color :error
               :title "Error"
               :class "mb-4"}
        "Something went wrong. Please try again."]

       [section-subtitle "Toast Notifications"]
       [demo-row
        [button {:on-click #(reset! show-success-toast true)
                 :start-icon lucide-icons/CheckCircle
                 :variant :outlined
                 :color :primary}
         "Show Success Toast"]

        [button {:on-click #(reset! show-error-toast true)
                 :start-icon lucide-icons/AlertCircle
                 :variant :outlined
                 :color :primary
                 :class "ml-4"}
         "Show Error Toast"]]

       (when @show-success-toast
         [toast {:variant :soft
                 :color :success
                 :auto-hide 3000
                 :on-hide #(reset! show-success-toast false)}
          "File uploaded successfully!"])

       (when @show-error-toast
         [toast {:variant :soft
                 :color :error
                 :auto-hide 3000
                 :on-hide #(reset! show-error-toast false)}
          "Failed to connect to server. Please try again."])])))

(defn toggles-section []
  (let [toggle-state (r/atom true)
        checkbox-state (r/atom true)
        indeterminate-state (r/atom true)
        radio-value (r/atom "option1")]
    (fn []
      [:div
       [section-title "Toggles & Switches"]

       [section-subtitle "Toggle Switch"]
       [demo-row
        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :label "Dark Mode"}]

        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :label "Airplane Mode"
                 :label-position :left
                 :class "ml-6"}]]

       [section-subtitle "Toggle Sizes"]
       [demo-row
        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :size :sm
                 :label "Small"}]

        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :size :md
                 :label "Medium"
                 :class "ml-6"}]

        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :size :lg
                 :label "Large"
                 :class "ml-6"}]]

       [section-subtitle "Toggle Colors"]
       [demo-row
        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :color :primary
                 :label "Primary"}]

        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :color :secondary
                 :label "Secondary"
                 :class "ml-6"}]

        [toggle {:checked @toggle-state
                 :on-change #(swap! toggle-state not)
                 :color :success
                 :label "Success"
                 :class "ml-6"}]]

       [section-subtitle "Checkboxes"]
       [demo-row
        [checkbox {:checked @checkbox-state
                   :on-change #(swap! checkbox-state not)
                   :label "Remember me"}]

        [checkbox {:checked false
                   :disabled true
                   :label "Disabled"
                   :class "ml-6"}]

        [checkbox {:indeterminate true
                   :on-change #(swap! indeterminate-state not)
                   :label "Indeterminate"
                   :class "ml-6"}]]

       [section-subtitle "Radio Buttons"]
       [demo-row
        [radio {:checked (= @radio-value "option1")
                :on-change #(reset! radio-value "option1")
                :name "radio-group"
                :value "option1"
                :label "Option 1"}]

        [radio {:checked (= @radio-value "option2")
                :on-change #(reset! radio-value "option2")
                :name "radio-group"
                :value "option2"
                :label "Option 2"
                :class "ml-6"}]

        [radio {:checked (= @radio-value "option3")
                :on-change #(reset! radio-value "option3")
                :name "radio-group"
                :value "option3"
                :label "Option 3"
                :class "ml-6"}]]])))

(defn dropdowns-section []
  [:div
   [section-title "Dropdowns & Menus"]

   [section-subtitle "Basic Dropdown"]
   [demo-row
    (r/with-let [open? (r/atom false)]
      [dropdown {:trigger [button {:variant :outlined} "Open Menu"]
                 :width "w-48"
                 :open? open?
                 :on-close #(reset! open? false)}
       [menu-item {:icon lucide-icons/Edit} "Edit"]
       [menu-item {:icon lucide-icons/Copy} "Duplicate"]
       [menu-item {:icon lucide-icons/Share2} "Share"]
       [menu-divider]
       [menu-item {:icon lucide-icons/Trash2
                   :danger true} "Delete"]])

    (r/with-let [open? (r/atom false)]
      [dropdown {:trigger [button {:variant :outlined} "Placement Top"]
                 :placement :top-left
                 :width "w-48"
                 :open? open?
                 :on-close #(reset! open? false)}
       [menu-item {:icon lucide-icons/Edit} "Edit"]
       [menu-item {:icon lucide-icons/Copy} "Duplicate"]
       [menu-item {:icon lucide-icons/Share2} "Share"]])]

   [section-subtitle "Dropdown with Groups"]
   (r/with-let [open? (r/atom false)]
     [dropdown {:trigger [button "Advanced Menu"]
                :placement :bottom-right
                :width "w-56"
                :open? open?
                :on-close #(reset! open? false)}

      [menu-label {} "Actions"]
      [menu-item {:icon lucide-icons/Edit} "Edit"]
      [menu-item {:icon lucide-icons/Copy} "Duplicate"]
      [menu-item {:icon lucide-icons/Share2} "Share"]

      [menu-divider]

      [menu-label {} "Export"]
      [menu-item {:icon lucide-icons/FileText} "As PDF"]
      [menu-item {:icon lucide-icons/Table} "As Excel"]
      [menu-item {:icon lucide-icons/Image} "As Image"]

      [menu-divider]

      [menu-item {:icon lucide-icons/Trash2
                  :danger true} "Delete Project"]])])

(defn skeletons-section []
  [:div
   [section-title "Skeletons & Loading States"]

   [:p {:class "mb-6 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
    "Skeleton components provide beautiful loading states while content is being fetched."]

   [section-subtitle "Basic Skeleton Variants"]
   [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8"}
    [:div {:class "p-6 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl shadow-sm"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Text Skeleton"]
     [skeleton {:variant :text :width "100%"}]
     [skeleton {:variant :text :width "80%" :class "mt-2"}]
     [skeleton {:variant :text :width "60%" :class "mt-2"}]]

    [:div {:class "p-6 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl shadow-sm"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Rectangular"]
     [skeleton {:variant :rectangular :width "100%" :height "120px"}]]

    [:div {:class "p-6 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl shadow-sm"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Circular"]
     [:div {:class "flex justify-center items-center h-[120px]"}
      [skeleton {:variant :circular :width "80px" :height "80px"}]]]

    [:div {:class "p-6 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl shadow-sm"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Avatar & Text"]
     [skeleton-avatar-with-text {:avatar-size "3.5rem"}]]]

   [section-subtitle "Animation Types"]
   [:div {:class "grid grid-cols-1 md:grid-cols-3 gap-6 mb-8"}
    [:div {:class "p-6 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl shadow-sm"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Pulse Animation"]
     [skeleton-text {:rows 3 :animation :pulse}]]

    [:div {:class "p-6 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl shadow-sm"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Wave Animation"]
     [skeleton-text {:rows 3 :animation :wave}]]

    [:div {:class "p-6 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-xl shadow-sm"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "No Animation"]
     [skeleton-text {:rows 3 :animation nil}]]]

   [section-subtitle "Realistic Examples"]
   [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8"}
    ;; Product card loading
    [:div
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Product Card"]
     [card {}
      [skeleton {:variant :rectangular :height "220px" :animation :wave}]
      [card-content {}
       [skeleton {:variant :text :width "70%" :height "1.5rem" :class "mb-3"}]
       [skeleton {:variant :text :width "40%" :height "1rem" :class "mb-4"}]
       [skeleton {:variant :text :width "90%" :class "mb-1"}]
       [skeleton {:variant :text :width "85%" :class "mb-4"}]
       [:div {:class "flex justify-between items-center"}
        [skeleton {:variant :text :width "25%" :height "1.5rem"}]
        [skeleton {:variant :rectangular :width "100px" :height "2rem" :class "rounded-lg"}]]]]]

    ;; User profile loading
    [:div
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "User Profile"]
     [card {}
      [card-content {}
       [:div {:class "flex flex-col items-center mb-4"}
        [skeleton {:variant :circular :width "100px" :height "100px" :class "mb-3"}]
        [skeleton {:variant :text :width "40%" :height "1.5rem" :class "mb-1"}]
        [skeleton {:variant :text :width "60%" :height "1rem"}]]
       [:div {:class "flex justify-around mb-4"}
        [:div {:class "flex-1 flex flex-col items-center"}
         [skeleton {:variant :text :width "30px" :height "1.5rem" :class "mb-1"}]
         [skeleton {:variant :text :width "40px" :height "1rem"}]]
        [:div {:class "flex-1 flex flex-col items-center"}
         [skeleton {:variant :text :width "30px" :height "1.5rem" :class "mb-1"}]
         [skeleton {:variant :text :width "40px" :height "1rem"}]]
        [:div {:class "flex-1 flex flex-col items-center"}
         [skeleton {:variant :text :width "30px" :height "1.5rem" :class "mb-1"}]
         [skeleton {:variant :text :width "40px" :height "1rem"}]]]
       [:div {:class "border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] pt-4"}
        [skeleton-text {:rows 2}]]]]]

    ;; Question set card loading
    [:div
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Question Set"]
     [card {}
      [card-content {}
       [:div {:class "flex justify-between items-start mb-4"}
        [:div {:class "flex-1"}
         [skeleton {:variant :text :width "70%" :height "1.5rem" :class "mb-2"}]
         [skeleton {:variant :text :width "90%" :class "mb-1"}]
         [skeleton {:variant :text :width "85%"}]]
        [:div {:class "flex-shrink-0 px-3 py-2 bg-[var(--color-light-bg)] dark:bg-[var(--color-dark-bg)] rounded-lg"}
         [skeleton {:variant :text :width "40px" :height "1.5rem" :class "mb-1"}]
         [skeleton {:variant :text :width "60px" :height "0.875rem"}]]]
       [:div {:class "flex flex-wrap gap-1 mb-4"}
        (for [i (range 3)]
          ^{:key i}
          [skeleton {:variant :rectangular :width (str (+ 40 (* i 10)) "px") :height "1.5rem" :class "rounded-full"}])]
       [:div {:class "flex justify-between items-center text-xs"}
        [skeleton {:variant :text :width "100px" :height "0.875rem"}]]]]]

    ;; Comment section loading
    [:div
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Comments Section"]
     [card {}
      [card-header {:title "Comments"
                    :subtitle [skeleton {:variant :text :width "40%" :height "1rem"}]}]
      [card-content {}
       [:div {:class "space-y-4"}
        [skeleton-avatar-with-text {:rows 2 :avatar-size "2.5rem"}]
        [skeleton-avatar-with-text {:rows 1 :avatar-size "2.5rem"}]
        [skeleton-avatar-with-text {:rows 3 :avatar-size "2.5rem"}]
        [:div {:class "border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] pt-4 mt-6"}
         [:div {:class "flex gap-3"}
          [skeleton {:variant :circular :width "2.5rem" :height "2.5rem"}]
          [skeleton {:variant :rectangular :width "100%" :height "4rem" :class "rounded-lg"}]]]]]]]

    ;; Data table loading
    [:div {:class "col-span-1 lg:col-span-2"}
     [:h4 {:class "text-sm mb-3 font-medium text-[var(--color-primary-600)] dark:text-[var(--color-primary-300)]"} "Data Table"]
     [card {}
      [card-header {:title "Study Sessions"
                    :subtitle "Recent activity"}]
      [card-content {}
       [skeleton-table {:rows 5 :cols 4}]
       [:div {:class "flex justify-center mt-6"}
        [skeleton {:variant :rectangular :width "300px" :height "2rem" :class "rounded-md"}]]]]]]])

(defn components-page []
  (fn []
    [:div.container.mx-auto.px-4.py-8
     [:h1.text-3xl.font-bold.mb-6
      {:class "text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}
      "Components Showcase"]
     [:p.text-lg.mb-8
      {:class "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"}
      "A collection of beautiful, reusable UI components for your ClojureScript application."]

     [buttons-section]
     [cards-section]
     [inputs-section]
     [badges-section]
     [alerts-section]
     [toggles-section]
     [dropdowns-section]
     [skeletons-section]]))