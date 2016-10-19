(ns metadata.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure.tools.logging :as log]))

(def default-config-file "/etc/iplant/de/metadata.properties")

(def docs-uri "/docs")

(def svc-info
  {:desc     "The REST API for the Discovery Environment Metadata services."
   :app-name "metadata"
   :group-id "org.cyverse"
   :art-id   "metadata"
   :service  "metadata"})

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(cc/defprop-optint listen-port
  "The port that metadata listens to."
  [props config-valid configs]
  "metadata.app.listen-port" 60000)

(cc/defprop-optstr environment-name
  "The name of the environment that this instance of Terrain belongs to."
  [props config-valid configs]
  "metadata.app.environment-name" "docker-compose")

;;;Database connection information
(cc/defprop-optstr db-driver-class
  "The name of the JDBC driver to use."
  [props config-valid configs]
  "metadata.db.driver" "org.postgresql.Driver")

(cc/defprop-optstr db-subprotocol
  "The subprotocol to use when connecting to the database (e.g. postgresql)."
  [props config-valid configs]
  "metadata.db.subprotocol" "postgresql")

(cc/defprop-optstr db-host
  "The host name or IP address to use when
   connecting to the database."
  [props config-valid configs]
  "metadata.db.host" "dedb")

(cc/defprop-optstr db-port
  "The port number to use when connecting to the database."
  [props config-valid configs]
  "metadata.db.port" "5432")

(cc/defprop-optstr db-name
  "The name of the database to connect to."
  [props config-valid configs]
  "metadata.db.name" "metadata")

(cc/defprop-optstr db-user
  "The username to use when authenticating to the database."
  [props config-valid configs]
  "metadata.db.user" "de")

(cc/defprop-optstr db-password
  "The password to use when authenticating to the database."
  [props config-valid configs]
  "metadata.db.password" "notprod")
;;;End database connection information

;;;AMQP connection information
(cc/defprop-optstr amqp-uri
  "The URI to connect to the AMQP server"
  [props config-valid configs]
  "metadata.amqp.uri" "amqp://guest:guestPW@localhost:5672")

(cc/defprop-optstr exchange-name
  "The exchange name for the AMQP server"
  [props config-valid configs]
  "metadata.amqp.exchange.name" "de")

(cc/defprop-optstr exchange-type
  "The name of the AMQP exchange type"
  [props config-valid configs]
  "metadata.amqp.exchange.type" "topic")

(cc/defprop-optboolean exchange-durable?
  "Whether or not the AMQP is durable."
  [props config-valid configs]
  "metadata.amqp.exchange.durable" true)


(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:type :clojure-commons.exception/invalid-cfg})))

(defn log-environment
  []
  (log/warn "ENV? metadata.db.host =" (db-host)))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props)
  (log-environment)
  (validate-config))
