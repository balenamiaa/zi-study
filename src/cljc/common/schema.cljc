(ns common.schema
  "Shared Malli schemas across Clojure/Script.

  Provides request/response contracts to improve robustness,
  enable coercion on the backend, and serve as living documentation."
  (:require [malli.core :as m]
            [malli.experimental.time :as malli.time]
            [malli.registry :as malli.registry]
            [malli.util :as mu]))

(malli.registry/set-default-registry!
 (malli.registry/composite-registry
  (m/default-schemas)
  (malli.time/schemas)))

(def email
  "A simple, pragmatic email matcher."
  [:re {:error/message "Must be a valid email"}
   #"^[^@\s]+@[^@\s]+\.[^@\s]+$"])

(def non-empty-string
  [:string {:min 1 :error/message "Must not be blank"}])

(def password
  "User password with minimal length requirements."
  [:string {:min 8}])

(def user-public
  "Public user payload returned by auth endpoints."
  [:map
   [:id :int]
   [:email email]
   [:name {:optional true} :string]
   [:provider {:optional true} [:maybe :string]]
   [:provider-id {:optional true} [:maybe :string]]
   [:avatar-url {:optional true} [:maybe :string]]
   [:has-password? {:optional true} :boolean]
   ;; Backends may stringify instants
   [:created-at {:optional true} [:maybe [:or :time/instant :string]]]])

(def register-request
  "Register endpoint body parameters."
  [:map
   [:email email]
   [:password password]
   [:name {:optional true} non-empty-string]])

(def login-request
  "Login endpoint body parameters."
  [:map
   [:email email]
   [:password non-empty-string]
   [:remember? {:optional true} :boolean]])

(def user-profile-update
  "User profile update payload."
  [:map
   [:name non-empty-string]])

(def change-password-request
  "Change password payload: when current password exists, it must be provided."
  [:map
   [:current {:optional true} :string]
   [:new password]
   [:confirm {:optional true} :string]])

(def todo
  [:map
   [:id :int]
   [:text :string]
   [:status [:enum :resolved :unresolved]]
   [:created-at :time/instant]])

(def new-todo
  (-> todo
      (mu/select-keys [:text])))

(def update-todo
  (-> todo
      (mu/select-keys [:text :status])
      (mu/optional-keys)))
