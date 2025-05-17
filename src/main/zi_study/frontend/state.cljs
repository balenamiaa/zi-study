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

;; Detailed Question Schemas (for question-data content and user answers)
;; These reflect the kebab-case structures now produced by the backend importer
;; and expected in frontend state.

(def McqQuestionContentSchema ; Covers mcq-single and mcq-multi
  [:map
   [:text :string]
   [:options [:vector :string]]
   [:correct-index {:optional true} :int] ; For mcq-single
   [:correct-indices {:optional true} [:vector :int]] ; For mcq-multi
   [:explanation {:optional true} [:maybe :string]]])

(def WrittenQuestionContentSchema
  [:map
   [:text :string]
   [:correct-answer {:optional true} [:maybe :string]] ; Correct answer might not always be part of question data initially
   [:explanation {:optional true} [:maybe :string]]])

(def TrueFalseQuestionContentSchema
  [:map
   [:text :string]
   [:is-correct-true :boolean]
   [:explanation {:optional true} [:maybe :string]]])

(def ClozeQuestionContentSchema
  [:map
   [:cloze-text :string]
   [:answers [:vector :string]]
   [:explanation {:optional true} [:maybe :string]]])

(def EmqPremiseSchema [:map [:temp-id {:optional true} :string] [:text :string]])
(def EmqOptionSchema [:map [:temp-id {:optional true} :string] [:text :string]])
(def EmqMatchSchema [:tuple :string :string]) ; temp-id to temp-id before processing, index to index after

(def EmqQuestionContentSchema
  [:map
   [:instructions {:optional true} [:maybe :string]]
   [:premises [:vector :string]] ; Stored as text only
   [:options [:vector :string]]  ; Stored as text only
   [:matches [:vector [:tuple :int :int]]] ; Stored as index to index
   [:explanation {:optional true} [:maybe :string]]])

(def QuestionDataSchema
  [:multi {:dispatch (fn [val _] (-> val :question-type keyword))} ; Dispatch on parent's :question-type
   [:mcq-single McqQuestionContentSchema]
   [:mcq-multi McqQuestionContentSchema]
   [:written WrittenQuestionContentSchema]
   [:true-false TrueFalseQuestionContentSchema]
   [:cloze ClozeQuestionContentSchema]
   [:emq EmqQuestionContentSchema]])

(def UserAnswerDataSchema ; Structure of :answer-data in :user-answer
  [:map ; This is generic, specific types might have more structure
   ;; For single/TF: {:answer boolean/int}
   ;; For multi: {:answer [int]}
   ;; For cloze: {:answer [string]}
   ;; For EMQ: {:answer [[int int]]} or {:answer {int int}}
   ;; For written: {:answer string}
   [:answer :any]])

(def UserAnswerSchema
  [:map
   [:answer-data UserAnswerDataSchema] ; Contains the user's actual input
   [:is-correct [:maybe :boolean]] ; Backend sends 0/1, http.cljs converts this to boolean
   [:submitted-at inst?]]) ; Or string if not parsed to inst? yet

(def QuestionSchema
  [:map
   [:question-id :int]
   [:set-id :int]
   [:question-set-title {:optional true} :string] ; Added in search results
   [:question-type :string] ; e.g., "mcq-single", "written"
   [:difficulty {:optional true} [:maybe :int]]
   [:question-data QuestionDataSchema] ; Parsed EDN content, now with kebab-case keys
   [:retention-aid {:optional true} [:maybe :string]]
   [:order-in-set {:optional true} [:maybe :int]]
   [:bookmarked :boolean]
   [:user-answer {:optional true} UserAnswerSchema]])

(def questions-registry-schema [:map-of :string QuestionSchema]) ;; Map of question-id (as string) -> question data

;; Folder Schemas
(def folder-item-schema
  [:map {:closed true}
   [:folder-id :int]
   [:user-id {:optional true} :int]
   [:name :string]
   [:description {:optional true} [:maybe :string]]
   [:is-public :boolean]
   [:created-at :string]
   [:updated-at :string]
   [:set-count {:optional true} :int]
   [:username {:optional true} :string]
   [:profile-picture-url {:optional true} [:maybe :string]]
   [:question-sets {:optional true} [:vector :map]]])

(def user-folders-list-schema
  [:map
   [:list [:vector folder-item-schema]]
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]])

(def public-folders-list-schema
  [:map
   [:list [:vector folder-item-schema]]
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]
   [:pagination [:map
                 [:page :int]
                 [:limit :int]
                 [:total_pages :int]
                 [:total_items :int]]]])

(def current-folder-details-schema
  [:map
   [:details {:optional true} [:maybe folder-item-schema]]
   [:loading? :boolean]
   [:error {:optional true} [:maybe :string]]
   [:managing-sets? :boolean] ; For operations like add/remove/reorder sets
   [:manage-sets-error {:optional true} [:maybe :string]]])

(def app-state-schema
  [:map
   [:auth auth-schema]
   [:ui ui-schema]
   [:router router-schema]
   [:question-bank question-bank-schema]
   [:advanced-search ; Key for the 'search all questions' state
    [:map ; Schema for the 'search all questions' state itself
     [:results advanced-search-results-schema]
     [:filters advanced-search-filters-schema]]]
   [:questions-registry questions-registry-schema]
   ;; Folders State
   [:folders [:map
              [:user-list user-folders-list-schema]
              [:public-list public-folders-list-schema]
              [:current-details current-folder-details-schema]]]])

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
                                       }}
           :questions-registry {}
           ;; Initial Folders State
           :folders {:user-list {:list []
                                 :loading? false
                                 :error nil}
                     :public-list {:list []
                                   :loading? false
                                   :error nil
                                   :pagination {:page 1
                                                :limit 10
                                                :total_items 0
                                                :total_pages 0}}
                     :current-details {:details nil
                                       :loading? false
                                       :error nil
                                       :managing-sets? false
                                       :manage-sets-error nil}}}))

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
  (r/reaction
   (let [question-ids (get-in @app-state [:question-bank :current-set :questions])
         registry (:questions-registry @app-state)]
     (mapv #(get registry (str %)) question-ids))))

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
  (r/reaction
   (let [results (get-in @app-state [:advanced-search :results])
         question-ids (:list results)
         registry (:questions-registry @app-state)
         questions (mapv #(get registry (str %)) question-ids)]
     (assoc results :list questions))))

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

(defn get-question-from-registry [question-id]
  (r/reaction (get-in @app-state [:questions-registry (str question-id)])))

(defn get-questions-from-registry [question-ids]
  (r/reaction (vals (select-keys (:questions-registry @app-state) (map str question-ids)))))

;; Registry updaters
(defn register-question
  "Add or update a question in the registry"
  [question]
  (let [question-id (str (:question-id question))]
    (swap! app-state assoc-in [:questions-registry question-id] question)))

(defn register-questions
  "Add or update multiple questions in the registry"
  [questions]
  (when (seq questions)
    (swap! app-state update :questions-registry
           (fn [registry]
             (reduce (fn [reg q]
                       (assoc reg (str (:question-id q)) q))
                     registry
                     questions)))))

(defn update-question-in-registry
  "Update specific fields in a question in the registry"
  [question-id updated-question-data]
  (swap! app-state update-in [:questions-registry (str question-id)]
         (fn [question]
           (if question
             (merge question updated-question-data)
             question))))

(defn remove-question-from-registry
  "Remove a question from the registry"
  [question-id]
  (swap! app-state update :questions-registry dissoc (str question-id)))

(defn clear-registry
  "Clear all questions from the registry"
  []
  (swap! app-state assoc :questions-registry {}))

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

    ;; Register the questions in the registry
    (register-questions questions)

    ;; Store only question IDs in the current-set
    (let [question-ids (mapv (comp str :question-id) questions)]
      (swap! app-state update :question-bank
             #(assoc % :current-set {:details set-details
                                     :questions question-ids
                                     :loading? false
                                     :questions-loading? false
                                     :questions-error nil
                                     :error nil
                                     ;; Reset filters when loading a new set
                                     :filters new-filters})))))

(defn set-current-set-questions [questions]
  ;; Register the questions in the registry
  (register-questions questions)

  ;; Store only question IDs in the current-set
  (let [question-ids (mapv (comp str :question-id) questions)]
    (swap! app-state update-in [:question-bank :current-set]
           #(assoc % :questions question-ids
                   :questions-loading? false
                   :questions-error nil))))

(defn update-current-question [question-id updated-question-data]
  ;; Update the question directly in the registry
  (update-question-in-registry question-id updated-question-data))

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
         auto-hide 2000
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

;; Search All Questions Updaters
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
  ;; Update the question directly in the registry
  (update-question-in-registry question-id updated-question-data))

(defn set-current-set-question-ids
  "Set question IDs for the current set and register the questions"
  [questions]
  (let [question-ids (mapv :question-id questions)]
    (register-questions questions)
    (swap! app-state assoc-in [:question-bank :current-set :questions] question-ids)
    (swap! app-state assoc-in [:question-bank :current-set :questions-loading?] false)
    (swap! app-state assoc-in [:question-bank :current-set :questions-error] nil)))

(defn set-advanced-search-question-ids
  "Set question IDs for 'search all questions' results and register the questions"
  [questions pagination]
  (let [question-ids (mapv (comp str :question-id) questions)]
    (register-questions questions)
    (swap! app-state update :advanced-search
           #(assoc % :results
                   {:list question-ids
                    :pagination pagination
                    :loading? false
                    :error nil}))))

;; Folder Selectors
(defn get-user-folders-list-state []
  (r/reaction (get-in @app-state [:folders :user-list])))

(defn get-public-folders-list-state []
  (r/reaction (get-in @app-state [:folders :public-list])))

(defn get-current-folder-details-state []
  (r/reaction (get-in @app-state [:folders :current-details])))

;; Folder Updaters
;; User Folders
(defn set-user-folders-loading [loading?]
  (swap! app-state assoc-in [:folders :user-list :loading?] loading?))

(defn set-user-folders-error [error]
  (swap! app-state assoc-in [:folders :user-list :error] error)
  (set-user-folders-loading false))

(defn set-user-folders-list [folders]
  (swap! app-state update-in [:folders :user-list]
         assoc :list folders :loading? false :error nil))

;; Public Folders
(defn set-public-folders-loading [loading?]
  (swap! app-state assoc-in [:folders :public-list :loading?] loading?))

(defn set-public-folders-error [error]
  (swap! app-state assoc-in [:folders :public-list :error] error)
  (set-public-folders-loading false))

(defn set-public-folders-list [folders pagination]
  (swap! app-state update-in [:folders :public-list]
         assoc :list folders :pagination pagination :loading? false :error nil))

(defn set-public-folders-page [page-num]
  (swap! app-state assoc-in [:folders :public-list :pagination :page] page-num))

;; Current Folder Details
(defn set-current-folder-details-loading [loading?]
  (swap! app-state assoc-in [:folders :current-details :loading?] loading?))

(defn set-current-folder-details-error [error]
  (swap! app-state assoc-in [:folders :current-details :error] error)
  (set-current-folder-details-loading false))

(defn set-current-folder-details [folder-details]
  (swap! app-state update-in [:folders :current-details]
         assoc :details folder-details :loading? false :error nil))

(defn clear-current-folder-details []
  (swap! app-state assoc-in [:folders :current-details]
         {:details nil :loading? false :error nil :managing-sets? false :manage-sets-error nil}))

(defn set-folder-managing-sets-state [folder-id loading? error]
  (when folder-id ;; Ensure we are talking about the current folder, or adjust if needed
    (swap! app-state update-in [:folders :current-details]
           assoc :managing-sets? loading? :manage-sets-error error)))

(defn add-folder-to-user-list
  "Adds a newly created folder to the beginning of the user's folder list"
  [folder]
  (swap! app-state update-in [:folders :user-list :list]
         (fn [current-list]
           (vec (concat [folder] (or current-list []))))))

(defn update-folder-in-user-list [updated-folder]
  (swap! app-state update-in [:folders :user-list :list]
         (fn [folders]
           (mapv #(if (= (:folder-id %) (:folder-id updated-folder))
                    updated-folder %)
                 folders))))

(defn remove-folder-from-user-list [folder-id]
  (swap! app-state update-in [:folders :user-list :list]
         (fn [folders]
           (vec (remove #(= (:folder-id %) folder-id) folders)))))

;; When a set is added to the current folder, update its question_sets and set_count
(defn add-set-to-current-folder-details [set-data]
  (swap! app-state update-in [:folders :current-details :details]
         (fn [details]
           (when details
             (-> details
                 (update :question_sets (fnil conj []) set-data) ; Add the new set
                 (update :set_count (fnil inc 0))))))) ; Increment set_count

;; When a set is removed from the current folder
(defn remove-set-from-current-folder-details [set-id]
  (swap! app-state update-in [:folders :current-details :details]
         (fn [details]
           (when details
             (-> details
                 (update :question_sets (fn [sets] (vec (remove #(= (:set_id %) set-id) sets))))
                 (update :set_count (fnil dec 0)))))))

;; When sets are reordered in the current folder
(defn reorder-sets-in-current-folder-details [ordered-sets]
  (swap! app-state update-in [:folders :current-details :details]
         (fn [details]
           (when details
             (assoc details :question_sets ordered-sets)))))
