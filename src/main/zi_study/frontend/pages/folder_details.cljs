(ns zi-study.frontend.pages.folder-details
  (:require
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   ["lucide-react" :as lucide-icons]
   [clojure.string :as str]
   [zi-study.frontend.routes :as routes]
   [zi-study.frontend.state :as state]
   [zi-study.frontend.utilities.http :as http]
   [zi-study.frontend.components.button :refer [button]]
   [zi-study.frontend.components.card :refer [card card-content card-footer card-header]]
   [zi-study.frontend.components.skeleton :refer [skeleton]]
   [zi-study.frontend.components.input :refer [text-input textarea]]
   [zi-study.frontend.components.modal :refer [modal]]
   [zi-study.frontend.components.toggle :refer [toggle]]
   [zi-study.frontend.components.dropdown :refer [dropdown menu-item menu-divider]]
   [zi-study.frontend.components.pagination :refer [pagination]]
   [zi-study.frontend.utilities :refer [cx]]))

;; --- Helper Components ---

(defn- set-card-progress [{:keys [progress]}]
  (when progress
    (let [{:keys [answered-percent correct-percent answered correct total]} progress
          rounded-answered-percent (Math/round (* 100 answered-percent))
          rounded-correct-percent (if (pos? answered) (Math/round (* 100 correct-percent)) 0)]
      [:div {:class "mt-auto pt-3"}
       [:div {:class "flex justify-between text-xs mb-1 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
        [:span (str "Progress: " rounded-answered-percent "%")]
        [:span (str answered "/" total)]]
       [:div {:class "h-2 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-full overflow-hidden mb-3"}
        [:div {:class "h-full bg-[var(--color-primary)] dark:bg-[var(--color-primary-400)] rounded-full transition-all duration-500 ease-out"
               :style {:width (str rounded-answered-percent "%")}}]]

       [:div {:class "flex justify-between text-xs mb-1 text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
        [:span (str "Accuracy: " (if (pos? answered) (str rounded-correct-percent "%") "N/A"))]
        [:span (str correct "/" answered)]]
       [:div {:class "h-2 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-full overflow-hidden"}
        [:div {:class (cx "h-full rounded-full transition-all duration-500 ease-out"
                          (if (pos? answered) "bg-[var(--color-success)] dark:bg-[var(--color-success-400)]" "bg-transparent"))
               :style {:width (str rounded-correct-percent "%")}}]]])))

(defn- set-card-menu [_props]
  (let [menu-open? (r/atom false)]
    (fn [{:keys [set-id on-remove on-move-up on-move-down is-first? is-last?]}]
      [dropdown {:open? menu-open?
                 :placement :bottom-center
                 :trigger
                 [:button {:class "p-1 rounded-full hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)] transition-colors"
                           :aria-label "Set options"}
                  [:> lucide-icons/MoreVertical {:size 18}]]}
       [menu-item {:on-click #(rfe/push-state routes/sym-set-page-route {:set-id set-id})
                   :class "select-none"
                   :start-icon lucide-icons/ExternalLink} "Open Set"]
       (when (or on-move-up on-move-down on-remove)
         [menu-divider])
       (when on-move-up
         [menu-item {:on-click #(on-move-up set-id) :disabled is-first?
                     :class "select-none"
                     :start-icon lucide-icons/ArrowUpCircle} "Move Up"])
       (when on-move-down
         [menu-item {:on-click #(on-move-down set-id) :disabled is-last?
                     :class "select-none"
                     :start-icon lucide-icons/ArrowDownCircle} "Move Down"])
       (when on-remove
         (when (or on-move-up on-move-down) [menu-divider])
         [menu-item {:on-click #(on-remove set-id) :danger true
                     :class "select-none"
                     :start-icon lucide-icons/Trash2} "Remove from Folder"])])))

(defn set-card [{:keys [set-data on-remove can-remove? on-move-up on-move-down is-first? is-last? folder-id]}]
  (let [{:keys [set-id title description total-questions progress]} set-data]
    [card {:class "h-full flex flex-col shadow hover:shadow-lg transition-all duration-200 bg-[var(--color-light-bg-card)] dark:bg-[var(--color-dark-bg-card)] cursor-pointer"
           :on-click #(rfe/push-state routes/sym-set-page-route {:set-id set-id} (when folder-id {:from-folder folder-id}))}
     [card-header {:title title
                   :subtitle (str total-questions " " (if (= 1 total-questions) "question" "questions"))
                   :action (when can-remove?
                             [set-card-menu {:set-id set-id :on-remove on-remove :can-remove? can-remove?
                                             :on-move-up on-move-up :on-move-down on-move-down
                                             :is-first? is-first? :is-last? is-last?}])}]
     [card-content {:class "flex-grow flex flex-col"}
      [:p {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mb-4 line-clamp-3 flex-grow"}
       (if (str/blank? description)
         [:span {:class "italic"} "No description provided."]
         description)]
      [set-card-progress {:progress progress}]]]))

;; --- Modal Components (Defined with def and r/create-class for state and lifecycle) ---

(def edit-folder-modal-internal
  (r/create-class
   {:display-name "edit-folder-modal-internal"
    :get-initial-state
    (fn [this]
      (let [current-folder (:folder (r/props this))]
        {:form-data (r/atom {:name (:name current-folder "")
                             :description (:description current-folder "")
                             :is-public (= (:is-public current-folder) 1)})
         :submitting? (r/atom false)}))

    :component-did-update
    (fn [this old-argv]
      (let [old-folder (:folder (second old-argv))
            new-folder (:folder (r/props this))
            form-data-atom (:form-data (r/state this))]
        (when (not= old-folder new-folder)
          (reset! form-data-atom {:name (:name new-folder "")
                                  :description (:description new-folder "")
                                  :is-public (:is-public new-folder false)}))))
    :reagent-render
    (fn [props]
      (let [component-state (r/state (r/current-component))
            {:keys [form-data submitting?]} component-state
            current-folder (:folder props)
            current-on-save (:on-save props)
            current-on-close (:on-close props)
            handle-submit-fn (fn []
                               (reset! submitting? true)
                               (http/update-folder (:folder-id current-folder) @form-data
                                                   (fn [updated-folder-data]
                                                     (reset! submitting? false)
                                                     (state/flash-success "Folder updated!")
                                                     (when current-on-save (current-on-save updated-folder-data))
                                                     (when current-on-close (current-on-close)))
                                                   (fn [error _error-data]
                                                     (reset! submitting? false)
                                                     (state/flash-error (str "Error updating folder: " error)))))]
        [modal {:show? true :on-close current-on-close :size :md :title "Edit Folder"
                :footer
                [:div {:class "flex justify-end gap-2"}
                 [button {:variant :text :on-click current-on-close :disabled @submitting?} "Cancel"]
                 [button {:on-click handle-submit-fn :loading @submitting? :disabled (or @submitting? (str/blank? (:name @form-data)))} "Save Changes"]]}
         [:div {:class "space-y-4 p-1"}
          [text-input {:label "Folder Name" :placeholder "E.g., Biology Chapter 5" :value (:name @form-data) :required true :on-change #(swap! form-data assoc :name (.. % -target -value))}]
          [textarea {:label "Description" :placeholder "(Optional) A brief summary of this folder's content" :rows 4 :value (:description @form-data) :on-change #(swap! form-data assoc :description (.. % -target -value))}]
          [toggle {:checked (:is-public @form-data) :on-change #(swap! form-data update :is-public not) :label "Make this folder public" :label-position :right}
           [:p {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mt-1 ml-9"} "Public folders are visible to everyone."]]]]))}))

(defn edit-folder-modal [props] [edit-folder-modal-internal props])

(def delete-folder-confirmation-modal-internal
  (r/create-class
   {:display-name "delete-folder-confirmation-modal-internal"
    :reagent-render
    (fn [props]
      (let [{:keys [on-confirm on-close loading?]} props]
        [modal
         {:show? true :on-close on-close :size :sm :title "Delete Folder"
          :footer
          [:div {:class "flex justify-end gap-2"}
           [button {:variant :text :on-click on-close :disabled @loading?} "Cancel"]
           [button {:variant :filled :on-click on-confirm :color :error :loading @loading?} "Delete Folder"]]}
         [:div {:class "p-1"}
          [:p "Are you sure you want to permanently delete this folder?"]
          [:p {:class "mt-2 text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
           "This action cannot be undone. The question sets within this folder will NOT be deleted."]]]))}))

(defn delete-folder-confirmation-modal [props] [delete-folder-confirmation-modal-internal props])

(defn- fetch-modal-sets-data [state-atoms props page-to-fetch query-term]
  (let [{:keys [pagination-state loading-available-sets available-sets-list]} state-atoms
        folder-id-to-exclude (:folder-id props)]
    (reset! loading-available-sets true)
    (http/get-sets-for-modal {:search query-term :exclude-sets-from-folder-id folder-id-to-exclude}
                             {:page page-to-fetch :limit (:limit @pagination-state)}
                             (fn [result]
                               (if (:success result)
                                 (let [data (:data result)
                                       api-pagination (:pagination data)]
                                   (reset! available-sets-list (:sets data))
                                   (swap! pagination-state assoc
                                          :page page-to-fetch
                                          :total_pages (or (:total_pages api-pagination) 0)
                                          :total_items (or (:total_items api-pagination) 0)))
                                 (state/flash-error (str "Could not load your question sets: " (:error result))))
                               (reset! loading-available-sets false)))))

(def add-sets-modal-internal
  (r/create-class
   {:display-name "add-sets-modal-internal"
    :get-initial-state
    (fn [_this]
      {:available-sets-list (r/atom [])       ; Stores the current page of sets
       :local-folder-set-ids (r/atom #{})    ; IDs of sets already in the current folder
       :selected-set-ids (r/atom #{})         ; IDs selected by the user across pages
       :pagination-state (r/atom {:page 1 :limit 10 :total_pages 0 :total_items 0})
       :loading-available-sets (r/atom true)
       :submitting-add (r/atom false)
       :search-query (r/atom "")})

    :component-did-mount
    (fn [this] ; `this` is the component instance
      (let [props (r/props this)
            state-atoms (r/state this)
            initial-folder-set-ids (:current-folder-set-ids props)]
        (reset! (:loading-available-sets state-atoms) true)
        (reset! (:selected-set-ids state-atoms) #{})
        (reset! (:search-query state-atoms) "")
        (reset! (:local-folder-set-ids state-atoms) (or initial-folder-set-ids #{}))
        (fetch-modal-sets-data state-atoms props 1 "")))

    ;; Correctly handle updates to current-folder-set-ids from props
    :component-did-update
    (fn [this old-argv]
      (let [old-props (second old-argv)
            new-props (r/props this)
            state-atoms (r/state this)
            old-folder-set-ids (:current-folder-set-ids old-props)
            new-folder-set-ids (:current-folder-set-ids new-props)
            local-folder-set-ids-atom (:local-folder-set-ids state-atoms)]
        (when (and local-folder-set-ids-atom (not= old-folder-set-ids new-folder-set-ids))
          (reset! local-folder-set-ids-atom (or new-folder-set-ids #{})))
        nil))

    :reagent-render
    (fn [props]
      (let [component (r/current-component)
            state-atoms (r/state component)
            {:keys [available-sets-list local-folder-set-ids selected-set-ids pagination-state loading-available-sets submitting-add search-query]} state-atoms
            current-on-close (:on-close props)
            current-folder-id (:folder-id props)

            handle-add-sets (fn []
                              (reset! submitting-add true)
                              (let [selected-ids @selected-set-ids
                                    sets-payload {:sets (map-indexed
                                                       (fn [idx set-id]
                                                         {:set-id set-id :order-in-folder idx})
                                                       (vec selected-ids))}]
                                (http/add-multiple-sets-to-folder!
                                 current-folder-id
                                 sets-payload
                                 (fn [_results]
                                   (reset! submitting-add false)
                                   (reset! selected-set-ids #{})
                                   (state/flash-success (str (count selected-ids) " set(s) added successfully."))
                                   (when (:on-success props) ((:on-success props)))
                                   (when current-on-close (current-on-close)))
                                 (fn [error]
                                   (reset! submitting-add false)
                                   (state/flash-error (str "Error adding sets: " error))))))

            handle-search-input-change (fn [event]
                                         ;; Just update the reactive atom for the input value
                                         (reset! search-query (.. event -target -value)))

            handle-debounced-search (fn [event]
                                      ;; This will be called after debounce period
                                      (let [current-query (.. event -target -value)]
                                        (fetch-modal-sets-data state-atoms props 1 current-query)))

            handle-page-change (fn [new-page]
                                 (fetch-modal-sets-data state-atoms props new-page @search-query))

            displayable-sets @available-sets-list]

        [modal
         {:show? true :on-close current-on-close :size :lg :title "Add Your Question Sets"
          :footer
          [:div {:class "flex justify-between items-center w-full"}
           [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
            (when (pos? (:total_items @pagination-state))
              (let [current-page (:page @pagination-state)
                    limit (:limit @pagination-state)
                    total-items (:total_items @pagination-state)
                    start-item (if (zero? total-items) 0 (inc (* (dec current-page) limit)))
                    end-item (min (* current-page limit) total-items)]
                (str "Showing " start-item "–" end-item " of " total-items)))]
           [:div {:class "flex justify-end gap-2"}
            [button {:variant :text :on-click current-on-close :disabled @submitting-add} "Cancel"]
            [button {:on-click handle-add-sets
                     :loading @submitting-add
                     :disabled (or (empty? @selected-set-ids) @submitting-add @loading-available-sets)}
             (str "Add " (count @selected-set-ids) " " (if (= 1 (count @selected-set-ids)) "Set" "Sets"))]]]}

         ^{:key "add-sets-modal-content"}
         [:div {:class "p-1 space-y-4"}
          [text-input {:placeholder "Search your sets by title or description..."
                       :start-icon lucide-icons/Search
                       :value @search-query
                       :on-change handle-search-input-change ;; Updates search-query atom immediately
                       :on-change-debounced {:time 300 :callback handle-debounced-search} ;; Triggers fetch after debounce
                       }]

          [:div {:class "border dark:border-[var(--color-dark-divider)] rounded-md min-h-[200px] max-h-[400px] overflow-y-auto divide-y dark:divide-[var(--color-dark-divider)]"}
           (cond
             @loading-available-sets
             (for [idx (range 3)] ; Show a few skeletons based on typical page limit
               ^{:key (str "skel-" idx)}
               [:div {:class "p-4 animate-pulse flex space-x-3 items-center"}
                [:div {:class "h-5 w-5 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded-md"}]
                [:div {:class "flex-1 space-y-1"}
                 [:div {:class "h-4 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded w-3/4"}]
                 [:div {:class "h-3 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded w-1/2"}]]])

             (empty? displayable-sets)
             [:div {:class "p-6 text-center text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
              [:> lucide-icons/Inbox {:size 32 :class "mx-auto mb-3 opacity-70"}]
              (cond
                (and @loading-available-sets (empty? displayable-sets))
                [:p "Loading..."]

                (and (empty? displayable-sets) (not (str/blank? @search-query)))
                [:p "No question sets match your search criteria that aren't already in this folder."]

                (empty? displayable-sets)
                [:p "All of your available sets are already in this folder, or no other sets were found."]

                :else
                [:p "No available question sets to add. Try a different search or create some sets first."])]

             :else
             (doall
              (for [{:keys [set-id title description total-questions]} displayable-sets]
                ^{:key set-id}
                [:div {:class (cx "p-3 flex items-center cursor-pointer transition-colors"
                                  (if (contains? @selected-set-ids set-id)
                                    "bg-[var(--color-light-bg-selected)] dark:bg-[var(--color-dark-bg-selected)] hover:bg-[var(--color-light-bg-selected)] dark:hover:bg-[var(--color-dark-bg-selected)]"
                                    "hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)]"))
                       :on-click #(swap! selected-set-ids (if (contains? @selected-set-ids set-id) disj conj) set-id)}
                 [:div {:class "flex-shrink-0 mr-3"}
                  [:div {:class (cx "w-5 h-5 rounded border-2 flex items-center justify-center transition-all"
                                    (if (contains? @selected-set-ids set-id)
                                      "bg-[var(--color-primary)] border-[var(--color-primary)] text-white"
                                      "border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-transparent"))}
                   (when (contains? @selected-set-ids set-id)
                     [:> lucide-icons/Check {:size 14 :stroke-width 3}])]]
                 [:div {:class "flex-grow min-w-0"}
                  [:div {:class "font-medium text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] truncate"} title]
                  [:p {:class "text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] truncate"}
                   (if (str/blank? description) "No description" description)]]
                 [:div {:class "flex-shrink-0 ml-3 text-xs text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}
                  (str total-questions "Q")]])))]

          (when (and (not @loading-available-sets) (pos? (:total_pages @pagination-state)) (> (:total_pages @pagination-state) 1))
            [:div {:class "mt-4"}
             [pagination {:page (:page @pagination-state)
                          :total-pages (:total_pages @pagination-state)
                          :total-items (:total_items @pagination-state)
                          :limit (:limit @pagination-state)
                          :on-page-change handle-page-change
                          :item-name (if (= 1 (:total_items @pagination-state)) "set" "sets")}]])]]))}))

(defn add-sets-modal [props] [add-sets-modal-internal props])

;; --- Main Page Component ---

(def folder-details-component
  (r/create-class
   {:display-name "folder-details-page"

    :get-initial-state
    (fn [_this]
      {:show-edit-modal (r/atom false)
       :show-delete-modal (r/atom false)
       :show-add-sets-modal (r/atom false)
       :deleting-folder? (r/atom false)})

    :component-did-mount
    (fn [this]
      (let [match (r/props this)
            folder-id (get-in match [:parameters :path :folder-id])]
        ;; First, clear any existing data to ensure proper loading state
        (state/clear-current-folder-details)
        ;; Then set loading state explicitly to true before making the request
        (state/set-current-folder-details-loading true)
        ;; Now fetch the folder details
        (http/get-folder-details folder-id nil)))

    :component-will-unmount
    (fn [_this]
      (state/clear-current-folder-details))

    :component-did-update
    (fn [this old-argv]
      (let [old-match (second old-argv)
            new-match (r/props this)
            old-folder-id (get-in old-match [:parameters :path :folder-id])
            new-folder-id (get-in new-match [:parameters :path :folder-id])]
        (when (not= old-folder-id new-folder-id)
          ;; Clear state and set loading to true before changing folders
          (state/clear-current-folder-details)
          (state/set-current-folder-details-loading true)
          (http/get-folder-details new-folder-id nil))))

    :reagent-render
    (fn [match]
      (let [component-local-state (r/state (r/current-component))
            folder-id (get-in match [:parameters :path :folder-id])

            {:keys [show-edit-modal show-delete-modal show-add-sets-modal deleting-folder?]} component-local-state

            folder-details-global-state @(state/get-current-folder-details-state)
            auth-state @(state/get-auth-state)

            {:keys [details loading? error managing-sets?]} folder-details-global-state
            current-user-id (get-in auth-state [:current-user :id])
            is-owner? (and details current-user-id (= (:user-id details) current-user-id))
            question-sets (get-in details [:question-sets] [])

            refresh-folder-details! (fn [] (http/get-folder-details folder-id nil))

            handle-folder-updated! (fn [_updated-data]
                                     (refresh-folder-details!)
                                     (reset! show-edit-modal false))

            handle-folder-deleted! (fn []
                                     (reset! deleting-folder? true)
                                     (http/delete-folder folder-id
                                                         (fn [_]
                                                           (reset! deleting-folder? false)
                                                           (state/flash-success "Folder deleted successfully.")
                                                           (rfe/push-state routes/sym-my-folders-route))
                                                         (fn [err _]
                                                           (reset! deleting-folder? false)
                                                           (state/flash-error (str "Error deleting folder: " err))
                                                           (reset! show-delete-modal false))))

            handle-remove-set! (fn [set-id-to-remove]
                                 (when (js/confirm "Are you sure you want to remove this set from the folder?")
                                   (http/remove-set-from-folder
                                    folder-id
                                    set-id-to-remove
                                    (fn [_]
                                      (state/flash-success "Set removed from folder.")
                                      (refresh-folder-details!))
                                    (fn [err _]
                                      (state/flash-error (str "Error removing set: " err))))))

            handle-reorder-set! (fn [set-id-to-move direction]
                                  (let [current-sets question-sets]
                                    (when (seq current-sets)
                                      (let [set-index (.findIndex current-sets #(= (:set-id %) set-id-to-move))]
                                        (when (and (>= set-index 0) (< set-index (count current-sets)))
                                          (let [new-index (if (= direction :up)
                                                            (max 0 (dec set-index))
                                                            (min (dec (count current-sets)) (inc set-index)))]
                                            (when (not= set-index new-index)
                                              (let [moved-set (nth current-sets set-index)
                                                    sets-without-moved (vec (concat (subvec current-sets 0 set-index)
                                                                                    (subvec current-sets (inc set-index))))
                                                    new-ordered-sets (vec (concat (subvec sets-without-moved 0 new-index)
                                                                                  [moved-set]
                                                                                  (subvec sets-without-moved new-index)))
                                                    sets-for-api (map-indexed (fn [idx s] {:set-id (:set-id s) :order-in-folder idx}) new-ordered-sets)]
                                                (state/reorder-sets-in-current-folder-details new-ordered-sets)
                                                (http/reorder-sets-in-folder folder-id sets-for-api
                                                                             (fn [_] (state/flash-success "Sets reordered."))
                                                                             (fn [err _]
                                                                               (state/flash-error (str "Error reordering sets: " err))
                                                                               (refresh-folder-details!)))))))))))

            render-header (fn []
                            [:div {:class "mb-8 pb-6 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
                             [:div {:class "mb-4"}
                              [button {:variant :text :size :sm :start-icon lucide-icons/ArrowLeft
                                       :class (cx "px-2 py-1 rounded-md transition-colors duration-150"
                                                  "text-[var(--color-light-back-button-text)] hover:text-[var(--color-light-back-button-text-hover)] hover:bg-[var(--color-light-back-button-bg-hover)]"
                                                  "dark:text-[var(--color-dark-back-button-text)] dark:hover:text-[var(--color-dark-back-button-text-hover)] dark:hover:bg-[var(--color-dark-back-button-bg-hover)]")
                                       :on-click #(rfe/push-state routes/sym-my-folders-route)}
                               "Back to My Folders"]]
                             (if details
                               [:div
                                [:div {:class "flex flex-col md:flex-row justify-between items-start gap-4"}
                                 [:div {:class "flex-grow min-w-0"}
                                  [:h1 {:class "text-3xl sm:text-4xl font-bold break-words text-[var(--color-light-text-heading)] dark:text-[var(--color-dark-text-heading)]"} (:name details)]
                                  [:div {:class "flex items-center flex-wrap mt-3 text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] gap-x-4 gap-y-1"}
                                   [:span {:class "flex items-center"}
                                    [:> (if (= 1 (:is-public details)) lucide-icons/Globe2 lucide-icons/LockKeyhole) {:size 16 :class "mr-1.5"}]
                                    (if (= 1 (:is-public details)) "Public" "Private")]
                                   (when-let [owner-email (:email details)]
                                     [:span {:class "flex items-center"}
                                      [:> lucide-icons/UserCircle2 {:size 16 :class "mr-1.5"}]
                                      (if is-owner? "By you" (str "By " owner-email))])
                                   [:span {:class "flex items-center"}
                                    [:> lucide-icons/Clock3 {:size 16 :class "mr-1.5"}]
                                    (str "Updated " (try (.toLocaleDateString (js/Date. (:updated-at details)) "en-US" #js {:month "short" :day "numeric" :year "numeric"}) (catch :default _ "recently")))]]]

                                 (when is-owner?
                                   [:div {:class "flex flex-col sm:flex-row flex-shrink-0 items-stretch sm:items-center gap-2 mt-2 md:mt-0"}
                                    [button {:variant :outlined :size :sm :start-icon lucide-icons/Edit3 :on-click #(reset! show-edit-modal true)} "Edit"]
                                    [button {:variant :outlined :size :sm :start-icon lucide-icons/Trash2 :color :error :on-click #(reset! show-delete-modal true)} "Delete"]])]

                                [:div {:class "mt-4"}
                                 (if (str/blank? (:description details))
                                   [:p {:class "italic text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"} "No description provided."]
                                   [:p {:class "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] whitespace-pre-wrap"} (:description details)])]]

                               ;; Skeleton for header
                               [:div {:class "space-y-3 animate-pulse"}
                                [:div {:class "h-8 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded w-1/2"}]
                                [:div {:class "h-4 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded w-1/3 mb-2"}]
                                [:div {:class "h-4 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded w-3/4"}]
                                [:div {:class "h-4 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] rounded w-full mt-1"}]])])

            render-sets-section (fn []
                                  [:section {:aria-labelledby "sets-in-folder-heading" :class "mt-2"}
                                   [:div {:class "flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6"}
                                    [:h2 {:id "sets-in-folder-heading" :class "text-2xl font-semibold text-[var(--color-light-text-heading)] dark:text-[var(--color-dark-text-heading)]"}
                                     "Question Sets"]
                                    (when is-owner?
                                      [button {:start-icon lucide-icons/PlusCircle
                                               :variant :outlined
                                               :class "mt-2 sm:mt-0"
                                               :on-click #(reset! show-add-sets-modal true)
                                               :disabled managing-sets?
                                               :loading managing-sets?}
                                       "Add Sets To Folder"])]

                                   (cond
                                     managing-sets?
                                     [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6"}
                                      (for [i (range (max 1 (count question-sets)))]
                                        ^{:key (str "managing-set-skeleton-" i)}
                                        [card {:class "h-64 animate-pulse rounded-lg"}
                                         [card-content {}
                                          [skeleton {:class "h-5 w-3/4 mb-2"}]
                                          [skeleton {:class "h-3 w-1/2 mb-4"}]
                                          [skeleton {:class "h-12 w-full"}]]])]

                                     (empty? question-sets)
                                     [:div {:class "flex flex-col items-center justify-center py-16 text-center border-2 border-dashed border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] rounded-lg mt-4 bg-[var(--color-light-bg-subtle)] dark:bg-[var(--color-dark-bg-subtle)]"}
                                      [:> lucide-icons/BookOpen {:size 48 :class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mb-4 opacity-60" :stroke-width 1.5}]
                                      [:h3 {:class "text-xl font-medium mb-2 text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]"} "This Folder is Empty"]
                                      [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] max-w-md mb-6"}
                                       (if is-owner?
                                         "Add some of your question sets to start filling it up!"
                                         "There are currently no question sets in this folder.")]]

                                     :else
                                     [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6"}
                                      (for [[idx set-data] (map-indexed vector question-sets)]
                                        ^{:key (:set-id set-data)}
                                        [set-card {:set-data set-data
                                                   :on-remove handle-remove-set!
                                                   :can-remove? is-owner?
                                                   :on-move-up (when is-owner? #(handle-reorder-set! (:set-id set-data) :up))
                                                   :on-move-down (when is-owner? #(handle-reorder-set! (:set-id set-data) :down))
                                                   :is-first? (zero? idx)
                                                   :is-last? (= idx (dec (count question-sets)))
                                                   :folder-id folder-id}])])])

            render-loading-state (fn []
                                   [:div {:class "animate-pulse"}
                                    [render-header]
                                    [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-8"}
                                     (for [i (range 3)]
                                       ^{:key (str "skeleton-set-" i)}
                                       [card {:class "h-64"}
                                        [card-content {}
                                         [skeleton {:class "h-6 w-3/4 mb-3"}]
                                         [skeleton {:class "h-4 w-1/2 mb-5"}]
                                         [skeleton {:class "h-16 w-full"}]]
                                        [card-footer {:class "border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
                                         [skeleton {:class "h-8 w-full"}]]])]])

            render-error-state (fn []
                                 [:div {:class "bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700/50 text-red-700 dark:text-red-300 rounded-lg p-6 text-center my-8"}
                                  [:> lucide-icons/AlertTriangle {:class "mx-auto h-12 w-12 mb-4 text-red-500 dark:text-red-400" :stroke-width 1.5}]
                                  [:h2 {:class "text-xl font-semibold mb-2"} "Failed to Load Folder"]
                                  [:p {:class "text-sm mb-4"} error]
                                  [button {:on-click refresh-folder-details! :variant :outlined :color :error :start-icon lucide-icons/RefreshCw}
                                   "Try Again"]])

            render-not-found-state (fn []
                                     [:div {:class "text-center py-20"}
                                      [:> lucide-icons/FolderSearch {:size 72 :class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] mb-6 opacity-50" :stroke-width 1}]
                                      [:h2 {:class "text-2xl font-semibold text-[var(--color-light-text-heading)] dark:text-[var(--color-dark-text-heading)] mb-2"} "Folder Not Found"]
                                      [:p {:class "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] max-w-md mx-auto mb-6"}
                                       "The folder you are looking for does not exist, has been moved, or you may not have permission to view it."]
                                      [button {:on-click #(rfe/push-state routes/sym-my-folders-route) :start-icon lucide-icons/Home}
                                       "Go to My Folders"]])]

        [:div {:class "container max-w-6xl mx-auto px-4 py-8 antialiased"}
         (cond
           loading? (render-loading-state)
           error (render-error-state)
           (and (not loading?) (not details)) (render-not-found-state)
           :else
           [:div
            [render-header]
            [render-sets-section]])

         (when (and @show-edit-modal details)
           [edit-folder-modal {:folder details
                               :on-save handle-folder-updated!
                               :on-close #(reset! show-edit-modal false)}])

         (when @show-delete-modal
           [delete-folder-confirmation-modal {:folder-id folder-id
                                              :loading? deleting-folder?
                                              :on-confirm handle-folder-deleted!
                                              :on-close #(reset! show-delete-modal false)}])

         (when @show-add-sets-modal
           [add-sets-modal {:folder-id folder-id
                            :current-folder-set-ids (set (map :set-id question-sets))
                            :on-success (fn [] (refresh-folder-details!) (reset! show-add-sets-modal false))
                            :on-close #(reset! show-add-sets-modal false)}])]))}))

(defn folder-details-page [match]
  [folder-details-component match])