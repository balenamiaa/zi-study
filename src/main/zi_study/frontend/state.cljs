(ns zi-study.frontend.state
  (:require [reagent.core :as r]))

;; Core application state
(defonce app-state
  (r/atom {:auth {:loading-current-user? false
                  :token nil
                  :authenticated? false
                  :current-user nil}
           :ui {:theme :system
                :flash {:messages []
                        :counter 0}} ;; Counter for unique IDs
           :router {:current-match nil}

           :question-bank
           {:sets {:list []
                   :loading? false
                   :error nil
                   :pagination {:page 1
                                :limit 10
                                :total_items 0
                                :total_pages 0}
                   :filters {:tags #{} ; Set of selected tag names
                             :search ""
                             :sort_by "created_at"
                             :sort_order "desc"
                             :show_bookmarked_sets false ; Toggle to show only sets with bookmarks
                             }}
            :current-set {:details nil ; Details of the specific set being viewed
                          :questions []
                          :questions-loading? false
                          :questions-error nil
                          :loading? false
                          :error nil
                          :filters {:difficulty nil
                                    :answered nil ; true, false, nil (any)
                                    :correct nil ; true, false, nil (any)
                                    :bookmarked nil ; true, false, nil (any)
                                    :search ""}}
            :tags {:list []
                   :loading? false
                   :error nil}
            :bookmarks {:list [] ; Grouped by set { :set-id ..., :set-title ..., :questions [...] }
                        :loading? false
                        :error nil}
            :answer-submission {; Track submission state per question
                                ;; :question-id-1 {:loading? true, :error "..."}
                                ;; :question-id-2 {:loading? false, :error nil}
                                }
            :bookmark-toggle {; Track toggle state per question
                              ;; :question-id-1 {:loading? true}
                              }
            :self-eval {; Track self-eval state per question
                        ;; :question-id-1 {:loading? true}
                        }}}))

;; Track the last applied filters to avoid redundant API calls
(defonce last-applied-filters (r/atom nil))

;; Selectors
(defn get-auth-state []
  (r/reaction (:auth @app-state)))

(defn get-ui-state []
  (r/reaction (:ui @app-state)))

(defn get-current-route []
  (get-in @app-state [:router :current-match]))

;; --- Question Bank Selectors ---
(defn get-qb-state []
  (r/reaction (:question-bank @app-state)))

(defn get-sets-list-state []
  (r/reaction (get-in @app-state [:question-bank :sets])))

(defn get-sets-filters []
  (r/reaction (get-in @app-state [:question-bank :sets :filters])))

(defn get-current-set-state []
  (r/reaction (get-in @app-state [:question-bank :current-set])))

(defn get-current-set-questions []
  (r/reaction (get-in @app-state [:question-bank :current-set :questions])))

(defn get-current-set-filters []
  (r/reaction (get-in @app-state [:question-bank :current-set :filters])))

(defn get-tags-state []
  (r/reaction (get-in @app-state [:question-bank :tags])))

(defn get-bookmarks-state []
  (r/reaction (get-in @app-state [:question-bank :bookmarks])))

(defn get-answer-submission-state [question-id]
  (r/reaction (get-in @app-state [:question-bank :answer-submission question-id])))

(defn get-bookmark-toggle-state [question-id]
  (r/reaction (get-in @app-state [:question-bank :bookmark-toggle question-id])))

(defn get-self-eval-state [question-id]
  (r/reaction (get-in @app-state [:question-bank :self-eval question-id])))

(defn get-last-applied-filters []
  "Returns an atom containing the last set of filters that were actually applied"
  last-applied-filters)

;; Auth state updaters
(defn set-auth-loading-current-user [loading?]
  (swap! app-state assoc-in [:auth :loading-current-user?] loading?))

(defn set-provisionally-authenticated
  "Sets authenticated to true based on token and cookie presence, current-user is nil initially."
  [token]
  (swap! app-state assoc-in [:auth]
         {:loading-current-user? true ; Will load user details next
          :authenticated? true
          :token token
          :current-user nil}))

(defn set-fully-authenticated "Sets authenticated to true and populates current-user."
  [token user]
  (swap! app-state assoc-in [:auth]
         {:loading-current-user? false
          :authenticated? true
          :token token
          :current-user user}))

(defn set-unauthenticated
  "Sets authenticated to false and clears user/token info."
  []
  (swap! app-state assoc-in [:auth]
         {:loading-current-user? false
          :authenticated? false
          :token nil
          :current-user nil}))

(defn reset-auth-state!
  "Resets the authentication part of the app state to initial values."
  []
  (set-unauthenticated))

;; Router state updaters
(defn set-current-route [match]
  (swap! app-state assoc-in [:router :current-match] match))

;; UI state updaters
(defn set-theme [theme]
  (swap! app-state assoc-in [:ui :theme] theme))

;; --- Question Bank Updaters ---

;; Sets List
(defn set-sets-loading [loading?]
  (swap! app-state assoc-in [:question-bank :sets :loading?] loading?))

(defn set-sets-error [error]
  (swap! app-state assoc-in [:question-bank :sets :error] error))

(defn set-sets-list [sets pagination]
  (swap! app-state update :question-bank
         #(assoc % :sets (-> (:sets %) ; Keep existing filters
                             (assoc :list sets
                                    :pagination pagination
                                    :loading? false
                                    :error nil)))))

(defn set-sets-filter-tags [tags-set]
  (swap! app-state assoc-in [:question-bank :sets :filters :tags] tags-set))

(defn set-sets-filter-search [search-term]
  (swap! app-state assoc-in [:question-bank :sets :filters :search] search-term))

(defn set-sets-filter-sort [sort-by sort-order]
  (swap! app-state assoc-in [:question-bank :sets :filters]
         (assoc (get-sets-filters) :sort_by sort-by :sort_order sort-order)))

(defn set-sets-filter-bookmarked [show?]
  (swap! app-state assoc-in [:question-bank :sets :filters :show_bookmarked_sets] show?))

(defn set-sets-page [page-num]
  (swap! app-state assoc-in [:question-bank :sets :pagination :page] page-num))

;; Current Set & Questions
(defn set-current-set-loading [loading?]
  (swap! app-state assoc-in [:question-bank :current-set :loading?] loading?))

(defn set-current-set-questions-loading [loading?]
  (swap! app-state assoc-in [:question-bank :current-set :questions-loading?] loading?))

(defn set-current-set-error [error]
  (swap! app-state assoc-in [:question-bank :current-set :error] error))

(defn set-current-set-questions-error [error]
  (swap! app-state assoc-in [:question-bank :current-set :questions-error] error)
  (swap! app-state assoc-in [:question-bank :current-set :questions-loading?] false))

(defn refresh-current-set-questions
  "Trigger a reload of the current set's questions using state parameters"
  []
  ;; This is a utility function that will be implemented by the HTTP layer
  ;; which will use the current set ID and filters to reload questions
  ;; We don't do anything directly here as the HTTP layer will call the state functions
  nil)

(defn set-current-set [set-details questions]
  (let [new-filters {:difficulty nil
                     :answered nil
                     :correct nil
                     :bookmarked nil
                     :search ""}]
    ;; Reset last-applied-filters when loading a new set
    (reset! last-applied-filters new-filters)
    (swap! app-state update :question-bank
           #(assoc % :current-set {:details set-details
                                   :questions questions
                                   :loading? false
                                   :questions-loading? false
                                   :questions-error nil
                                   :error nil
                                   ;; Reset filters when loading a new set
                                   :filters new-filters}))))

(defn set-current-set-questions [questions]
  (swap! app-state update-in [:question-bank :current-set]
         #(assoc % :questions questions
                 :questions-loading? false
                 :questions-error nil)))

(defn update-current-question [question-id updated-question-data]
  (swap! app-state update-in [:question-bank :current-set :questions]
         (fn [questions] (mapv (fn [q] (if (= (:question-id q) question-id) (merge q updated-question-data) q)) questions))))

(defn set-current-set-filters [filters-map]
  (swap! app-state assoc-in [:question-bank :current-set :filters] filters-map))


;; Tags List
(defn set-tags-loading [loading?]
  (swap! app-state assoc-in [:question-bank :tags :loading?] loading?))

(defn set-tags-error [error]
  (swap! app-state assoc-in [:question-bank :tags :error] error))

(defn set-tags-list [tags]
  (swap! app-state assoc-in [:question-bank :tags] {:list tags :loading? false :error nil}))

;; Bookmarks List
(defn set-bookmarks-loading [loading?]
  (swap! app-state assoc-in [:question-bank :bookmarks :loading?] loading?))

(defn set-bookmarks-error [error]
  (swap! app-state assoc-in [:question-bank :bookmarks :error] error))

(defn set-bookmarks-list [bookmarks]
  (swap! app-state assoc-in [:question-bank :bookmarks] {:list bookmarks :loading? false :error nil}))

;; Answer Submission State
(defn set-answer-submitting [question-id submitting? error]
  (swap! app-state assoc-in [:question-bank :answer-submission question-id] {:loading? submitting? :error error}))

;; Bookmark Toggle State
(defn set-bookmark-toggling [question-id toggling?]
  (swap! app-state assoc-in [:question-bank :bookmark-toggle question-id] {:loading? toggling?}))

;; Self-Eval State
(defn set-self-evaluating [question-id evaluating?]
  (swap! app-state assoc-in [:question-bank :self-eval question-id] {:loading? evaluating?}))

;; Flash message state selectors
(defn get-flash-messages []
  (r/reaction (get-in @app-state [:ui :flash :messages])))

;; Flash message state updaters
(defn add-flash-message
  "Add a flash message to the UI
  
   Options:
   - message: (required) message text or hiccup to display
   - color: :primary, :success, :warning, :error, :info (default :info)
   - variant: :filled, :outlined, :soft (default :soft)
   - auto-hide: milliseconds before auto-hiding (default 5000, nil for no auto-hide)
   - position: :top-left, :top-center, :top-right, :bottom-left, :bottom-center, :bottom-right (default :top-right)"
  [{:keys [message color variant auto-hide position]
    :or {color :info
         variant :soft
         auto-hide 5000
         position :top-right}
    :as opts}]
  (let [id (swap! app-state update-in [:ui :flash :counter] inc)]
    (swap! app-state update-in [:ui :flash :messages]
           conj (assoc opts
                       :id id
                       :color color
                       :variant variant
                       :auto-hide auto-hide
                       :position position))
    id)) ;; Return the message ID for potential reference

(defn remove-flash-message
  "Remove a flash message by ID"
  [id]
  (swap! app-state update-in [:ui :flash :messages]
         (fn [messages] (vec (remove #(= (:id %) id) messages)))))

(defn clear-flash-messages
  "Clear all flash messages"
  []
  (swap! app-state assoc-in [:ui :flash :messages] []))

;; Success/Error/Warning/Info convenience methods
(defn flash-success
  "Show a success flash message"
  [message & {:as opts}]
  (add-flash-message (merge {:message message :color :success} opts)))

(defn flash-error
  "Show an error flash message"
  [message & {:as opts}]
  (add-flash-message (merge {:message message :color :error} opts)))

(defn flash-warning
  "Show a warning flash message"
  [message & {:as opts}]
  (add-flash-message (merge {:message message :color :warning} opts)))

(defn flash-info
  "Show an info flash message"
  [message & {:as opts}]
  (add-flash-message (merge {:message message :color :info} opts)))