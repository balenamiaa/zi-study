(ns backend.db
  (:require [camel-snake-kebab.core :as csk]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs])
  (:import [java.sql PreparedStatement]))

(def opts
  "Default next.jdbc options."
  {:builder-fn rs/as-unqualified-kebab-maps
   :column-fn csk/->snake_case})

(extend-protocol prepare/SettableParameter
  clojure.lang.Keyword
  (set-parameter [k ^PreparedStatement s i]
    (.setObject s i (name k))))