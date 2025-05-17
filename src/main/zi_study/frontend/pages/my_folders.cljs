(ns zi-study.frontend.pages.my-folders
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]
   [clojure.string :as str]
   [zi-study.frontend.state :as state]
   [zi-study.frontend.utilities.http :as http]
   [zi-study.frontend.components.button :refer [button]]
   [zi-study.frontend.components.card :refer [card card-content card-header]]
   [zi-study.frontend.components.skeleton :refer [skeleton-text]]
   [zi-study.frontend.components.input :refer [text-input textarea]]
   [zi-study.frontend.components.modal :refer [modal]]
   [zi-study.frontend.components.folder-display-card :refer [folder-display-card]]
   [zi-study.frontend.components.toggle :refer [toggle]]))

(defn create-folder-modal [{:keys [on-close]}]
  (let [initial-form {:name ""
                      :description ""
                      :is-public false}
        form-data (r/atom initial-form)
        submitting? (r/atom false)

        handle-submit (fn []
                        (reset! submitting? true)
                        (http/create-folder @form-data
                                            (fn [_created-folder]
                                              (reset! submitting? false)
                                              (reset! form-data initial-form)
                                              (on-close)
                                              (state/flash-success "Folder created successfully!"))
                                            (fn [error _]
                                              (reset! submitting? false)
                                              (state/flash-error (str "Failed to create folder: " error)))))]
    (fn [{:keys [show? on-close]}]
      [modal {:show? show?
              :on-close on-close
              :size :md
              :title "Create New Folder"
              :footer
              [:div {:class "flex justify-end gap-2"}
               [button {:variant :text
                        :on-click on-close
                        :disabled @submitting?}
                "Cancel"]
               [button {:on-click handle-submit
                        :loading @submitting?
                        :disabled (or @submitting? (str/blank? (:name @form-data)))
                        :form "create-folder-form"}
                "Create Folder"]]}
       ^{:key "create-folder-modal-content"}
       [:div {:class "space-y-4" :id "create-folder-form"}
        [text-input {:label "Folder Name"
                     :placeholder "Enter a name for your folder"
                     :value (:name @form-data)
                     :required true
                     :on-change #(swap! form-data assoc :name (.. % -target -value))}]
        [textarea {:label "Description (optional)"
                   :placeholder "Describe what this folder contains"
                   :rows 3
                   :value (:description @form-data)
                   :on-change #(swap! form-data assoc :description (.. % -target -value))}]
        [toggle {:checked (:is-public @form-data)
                 :on-change #(swap! form-data update :is-public not)
                 :label "Make this folder public"
                 :label-position :right}
         [:div {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mt-1 ml-9"}
          "Public folders are visible to all users"]]]])))

(defn folder-grid [{:keys [folders loading? show-create-modal]}]
  [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-6"}
   (cond
     loading?
     (for [i (range 6)]
       ^{:key (str "skeleton-" i)}
       [card {:class "h-64"}
        [card-header {:title [skeleton-text {:width "70%"}]
                      :subtitle [skeleton-text {:width "30%"}]}]
        [card-content
         [:div {:class "space-y-2"}
          [skeleton-text {:width "100%"}]
          [skeleton-text {:width "100%"}]
          [skeleton-text {:width "60%"}]]]])

     (empty? folders)
     [:div {:class "col-span-full flex flex-col items-center justify-center py-12 text-center"}
      [:> lucide-icons/FolderOpen {:size 64 :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mb-4 opacity-50"}]
      [:h3 {:class "text-xl font-medium mb-2"} "No folders yet"]
      [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] max-w-md mb-6"}
       "Create your first folder to organize your question sets."]]

     :else
     (for [folder folders]
       ^{:key (:folder-id folder)}
       [folder-display-card {:folder folder}]))])

(defn my-folders-page []
  (let [show-create-modal (r/atom false)
        folders-state (state/get-user-folders-list-state)]

    (r/create-class
     {:component-did-mount
      (fn [_]
        (http/get-user-folders nil))

      :reagent-render
      (fn []
        (let [{:keys [list loading? error]} @folders-state]
          [:div {:class "container max-w-7xl mx-auto px-4 py-8"}
           ;; Header section
           [:div {:class "flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8"}
            [:div
             [:h1 {:class "text-2xl sm:text-3xl font-bold mb-2"} "My Folders"]
             [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
              "Organize your question sets into folders for better organization"]]
            [button {:on-click #(reset! show-create-modal true)
                     :start-icon lucide-icons/FolderPlus}
             "Create New Folder"]]

           ;; Error message if any
           (when error
             [:div {:class "bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 rounded-lg p-4 mb-6"}
              [:div {:class "flex items-start"}
               [:> lucide-icons/AlertCircle {:className "flex-shrink-0 mr-2 h-5 w-5"}]
               [:div
                [:p {:class "font-medium"} "Failed to load folders"]
                [:p {:class "text-sm mt-1"} error]]]])

           ;; Folders grid
           [folder-grid {:folders list :loading? loading? :show-create-modal show-create-modal}]

           ;; Create Folder Modal
           (when @show-create-modal
             [create-folder-modal {:show? @show-create-modal :on-close #(reset! show-create-modal false)}])]))})))