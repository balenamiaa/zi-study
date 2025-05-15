(ns zi-study.frontend.state
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [reagent.core :as r]))

;; State schema definitions
(def auth-schema
  [:map
   [:loading-current-user? :boolean]
   [:token {:optional true} [:maybe :string]]
   [:authenticated? :boolean]
   [:current-user {:optional true} [:maybe :map]]])

(def ui-schema
  [:map
   [:theme [:enum :system :light :dark]]
   [:flash [:map
            [:messages [:vector :map]]
            [:counter :int]]]])

(def router-schema
  [:map
   [:current-match {:optional true} [:maybe :map]]])

(def sets-filters-schema
  [:map
   [:tags [:set :string]]
   [:search :string]
   [:sort_by :string]
   [:sort_order :string]
   [:show_bookmarked_sets :boolean]])

(def sets-schema
  [:map
   [:list [:vector :map]]
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]
   [:pagination [:map
                 [:page :int]
                 [:limit :int]
                 [:total_pages :int]
                 [:total_items :int]]]
   [:filters sets-filters-schema]])

(def current-set-filters-schema
  [:map
   [:difficulty {:optional true} [:maybe :int]]
   [:answered {:optional true} [:maybe :boolean]]
   [:correct {:optional true} [:maybe :boolean]]
   [:bookmarked {:optional true} [:maybe :boolean]]
   [:search :string]])

(def current-set-schema
  [:map
   [:details {:optional true} [:maybe :map]]
   [:questions [:vector :map]]
   [:questions-loading? :boolean]
   [:questions-error {:optional true} [:maybe :string]]
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]
   [:filters current-set-filters-schema]])

(def tags-schema
  [:map
   [:list [:vector :string]]
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]])

(def bookmarks-schema
  [:map
   [:list [:vector :map]]
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]])

(def question-bank-schema
  [:map
   [:sets sets-schema]
   [:current-set current-set-schema]
   [:tags tags-schema]
   [:bookmarks bookmarks-schema]
   [:answer-submission [:map-of :any :map]]
   [:bookmark-toggle [:map-of :any :map]]
   [:self-eval [:map-of :any :map]]])

(def advanced-search-filters-schema
  [:map
   [:keywords :string]
   ;; Future: Add other filters like tags, difficulty, types
   ;; [:tags {:optional true} [:set :string]]
   ;; [:difficulty {:optional true} [:maybe :int]]
   ;; [:types {:optional true} [:vector QuestionType]] ;; Assuming QuestionType is defined or imported
   ])

(def advanced-search-results-schema
  [:map
   [:list [:vector :map]] ; Will store question objects with set_title etc.
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]
   [:pagination [:map
                 [:page :int]
                 [:limit :int]
                 [:total_pages :int]
                 [:total_items :int]]]])

(def app-state-schema
  [:map
   [:auth auth-schema]
   [:ui ui-schema]
   [:router router-schema]
   [:question-bank question-bank-schema]
   [:advanced-search ; Key for the advanced search state
    [:map ; Schema for the advanced search state itself
     [:results advanced-search-results-schema]
     [:filters advanced-search-filters-schema]]]])

;; Core application state
(defonce app-state
  (r/atom {:auth {:loading-current-user? false
                  :token nil
                  :authenticated? false
                  :current-user nil}
           :ui {:theme :system
                :flash {:messages []
                        :counter 0}}
           :router {:current-match nil}

           :question-bank
           {:sets {:list []
                   :loading? true
                   :error nil
                   :pagination {:page 1
                                :limit 12
                                :total_items 0
                                :total_pages 0}
                   :filters {:tags #{}
                             :search ""
                             :sort_by "created_at"
                             :sort_order "desc"
                             :show_bookmarked_sets false}}
            :current-set {:details nil
                          :questions []
                          :questions-loading? true
                          :questions-error nil
                          :loading? true
                          :error nil
                          :filters {:difficulty nil
                                    :answered nil
                                    :correct nil
                                    :bookmarked nil
                                    :search ""}}
            :tags {:list []
                   :loading? true
                   :error nil}
            :bookmarks {:list []
                        :loading? false
                        :error nil}
            :answer-submission {}
            :bookmark-toggle {}
            :self-eval {}}
           :advanced-search {:results {:list []
                                       :loading? false
                                       :error nil
                                       :pagination {:page 1
                                                    :limit 15
                                                    :total_items 0
                                                    :total_pages 0}}
                             :filters {:keywords ""
                                       ; Initialize future filters if added
                                       }}}))

;; Track the last applied filters to avoid redundant API calls
(defonce last-applied-filters (r/atom nil))
(defonce last-applied-advanced-search-filters (r/atom nil)) ;; For advanced search

;; Selectors
(defn get-auth-state []
  (r/reaction (:auth @app-state)))

(defn get-ui-state []
  (r/reaction (:ui @app-state)))

(defn get-current-route []
  (r/reaction (get-in @app-state [:router :current-match])))

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

(defn get-advanced-search-results-state []
  (r/reaction (get-in @app-state [:advanced-search :results])))

(defn get-advanced-search-results-pagination-state []
  (r/reaction (get-in @app-state [:advanced-search :results :pagination])))

(defn get-advanced-search-filters-state []
  (r/reaction (get-in @app-state [:advanced-search :filters])))

(defn get-last-applied-filters
  "Returns an atom containing the last set of filters that were actually applied for current set questions"
  []
  last-applied-filters)

(defn get-last-applied-advanced-search-filters []
  last-applied-advanced-search-filters)

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
  
   Options scheme:
   [:map
    [:message {:doc \"message text or hiccup to display\"} [:or :string :vector]]
    [:color {:doc \"message color\", :optional true, :default :info} 
            [:enum :primary :success :warning :error :info]]
    [:variant {:doc \"message style\", :optional true, :default :soft} 
              [:enum :filled :outlined :soft]]
    [:auto-hide {:doc \"milliseconds before auto-hiding, nil for no auto-hide\", 
                 :optional true, :default 5000} 
                [:maybe :int]]
    [:position {:doc \"message position\", :optional true, :default :top-right}
               [:enum :top-left :top-center :top-right 
                      :bottom-left :bottom-center :bottom-right]]]"
  [{:keys [message color variant auto-hide position]
    :or {color :info
         variant :soft
         auto-hide 5000
         position :top-right}
    :as opts}]
  (let [id (swap! app-state update-in [:ui :flash :counter] inc)]
    (swap! app-state update-in [:ui :flash :messages]
           conj (assoc opts
                       :message message
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

;; =========== Malli Schema Validation Utilities ===========

(defn validate-state-update
  "Validates a state update against a schema segment. 
   Returns [true nil] if valid, [false error-message] if invalid."
  [schema data & {:keys [humanize]
                  :or {humanize true}}]
  (let [valid? (m/validate schema data)]
    (if valid?
      [true nil]
      (let [explain-data (m/explain schema data)
            error-msg (if humanize
                        (str "Invalid state data: "
                             (pr-str (me/humanize explain-data)))
                        explain-data)]
        [false error-msg]))))

(defn safe-update-state!
  "Updates app-state only if the update is valid according to the schema.
   Otherwise logs an error. Returns true if update was successful, false otherwise."
  [path schema new-value]
  (let [[valid? error] (validate-state-update schema new-value)]
    (if valid?
      (do
        (swap! app-state assoc-in path new-value)
        true)
      (do
        (js/console.error "State update validation failed for path" path ":" error)
        false))))

;; In development mode, we could add Schema validation to every state change:
;; (add-watch app-state :schema-validator
;;   (fn [_ _ _ new-state]
;;     (when-not (m/validate app-state-schema new-state)
;;       (let [explain-data (m/explain app-state-schema new-state)]
;;         (js/console.error "App state schema validation failed:" (m/humanize explain-data))))))

;; Advanced Search Updaters
(defn set-advanced-search-loading [loading?]
  (swap! app-state assoc-in [:advanced-search :results :loading?] loading?))

(defn set-advanced-search-error [error]
  (swap! app-state update :advanced-search
         (fn [old]
           (-> (assoc-in old [:results :error] error)
               (assoc-in [:results :loading?] false)))))


(defn set-advanced-search-results [results pagination]
  (swap! app-state update :advanced-search
         #(assoc % :results {:list results
                             :pagination pagination
                             :loading? false
                             :error nil})))

(defn set-advanced-search-keywords [keywords-str]
  (swap! app-state assoc-in [:advanced-search :filters :keywords] keywords-str))

(defn set-advanced-search-page [page-num]
  (swap! app-state assoc-in [:advanced-search :results :pagination :page] page-num))

(defn update-advanced-search-question [question-id updated-question-data]
  (swap! app-state update-in [:advanced-search :results :list]
         (fn [questions] (mapv (fn [q] (if (= (:question-id q) question-id) (merge q updated-question-data) q)) questions))))