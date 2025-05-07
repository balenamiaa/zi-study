(ns zi-study.backend.db
  (:require [migratus.core :as migratus]
            [clojure.java.io :as io])
  (:import [java.sql SQLException]
           [com.zaxxer.hikari HikariDataSource HikariConfig]))

(defonce db-pool (atom nil))

(def migratus-config
  {:store                :database
   :migration-dir        "migrations/"
   :migration-table-name "schema_migrations"
   :db                   {:datasource @db-pool}})

(defn- hikari-config [db-path]
  (let [config-map {; Clojure style keys
                    :jdbc-url           (str "jdbc:sqlite:" db-path)
                    :driver-class-name  "org.sqlite.JDBC" ; Specify driver for SQLite
                    :maximum-pool-size  20
                    :minimum-idle       10
                    :idle-timeout       600000
                    :connection-timeout 30000
                    :validation-timeout 5000
                    :max-lifetime       1800000
                    :pool-name          "zistudy-db-pool"
                    :auto-commit        true}]
    (doto (HikariConfig.)
      (.setJdbcUrl (:jdbc-url config-map))
      (.setDriverClassName (:driver-class-name config-map))
      (.setMaximumPoolSize (:maximum-pool-size config-map))
      (.setMinimumIdle (:minimum-idle config-map))
      (.setIdleTimeout (:idle-timeout config-map))
      (.setConnectionTimeout (:connection-timeout config-map))
      (.setValidationTimeout (:validation-timeout config-map))
      (.setMaxLifetime (:max-lifetime config-map))
      (.setPoolName (:pool-name config-map))
      (.setAutoCommit (:auto-commit config-map)))))

(defn init-db! []
  (println "Initializing database connection pool...")
  (try
    (when-not @db-pool
      (let [db-path "db/zistudy_dev.db"
            _ (-> (io/file db-path) (.getParentFile) (.mkdirs))
            ^HikariConfig config (hikari-config db-path)
            datasource (HikariDataSource. config)]
        (reset! db-pool datasource)
        (println "Running database migrations...")
        (let [current-migratus-config (assoc migratus-config
                                             :db {:datasource @db-pool})]
          (migratus/migrate current-migratus-config))
        (println "Database initialization and migrations complete.")))
    (catch SQLException e
      (println "Database initialization failed:" (ex-message e))
      (.printStackTrace e)
      (throw e))
    (catch Exception e
      (println "An unexpected error occurred during DB initialization:" (ex-message e))
      (.printStackTrace e)
      (throw e))))


(defn close-db! []
  (when-let [^HikariDataSource ds @db-pool]
    (println "Closing database connection pool...")
    (.close ds)
    (reset! db-pool nil)
    (println "Database connection pool closed.")))
