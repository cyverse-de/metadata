(ns metadata.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [metadata.util.db :as db]
            [metadata.util.config :as config]
            [metadata.amqp :as amqp]
            [metadata.events :as events]
            [service-logging.thread-context :as tc]))

(defn init-amqp
  []
  (let [amqp-handlers {"events.metadata.ping" events/ping-handler}
        exchange-cfg  (events/exchange-config)
        queue-cfg     (events/queue-config)
        channel       (amqp/connect exchange-cfg queue-cfg (keys amqp-handlers))]
    (amqp/channel channel)
    (.start (Thread. (fn [] (amqp/subscribe channel (:name queue-cfg) amqp-handlers))))))

(defn init-service
  [& [cfg-path]]
  (config/load-config-from-file (or cfg-path config/default-config-file))
  (init-amqp)
  (db/define-database))

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default config/default-config-file
    :validate [#(fs/exists? %) "The config file does not exist."
               #(fs/readable? %) "The config file is not readable."]]
   ["-p" "--port PORT"
    :parse-fn #(Integer/parseInt %)]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn run-jetty
  [port]
  (require 'metadata.routes
           'ring.adapter.jetty)
  (log/warn "Started listening on" (config/listen-port))
  ((eval 'ring.adapter.jetty/run-jetty) (eval 'metadata.routes/app) {:port (or port (config/listen-port))}))

(defn -main
  [& args]
  (tc/with-logging-context config/svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args config/svc-info args cli-options)]
      (init-service (:config options))
      (run-jetty (:port options)))))
