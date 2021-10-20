(ns metadata.util.db
  (:use [metadata.util.config]
        [korma.db])
  (:require [next.jdbc :as jdbc]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the metadata database using Korma."
  []
  (postgres
    {:subprotocol (db-subprotocol)
     :host        (db-host)
     :port        (db-port)
     :db          (db-name)
     :user        (db-user)
     :password    (db-password)}))

(defn- create-db-spec-nextjdbc
  "Create the database connection spec to use when accessing the database using next.jdbc"
  []
  {:dbtype "postgresql" :host (db-host) :port (db-port) :dbname (db-name) :user (db-user) :password (db-password)})

(declare ds)

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (def ds (jdbc/get-datasource (create-db-spec-nextjdbc)))
  (defdb metadata (create-db-spec)))

(defn t
  [tname]
  (keyword (db-schema) tname))
