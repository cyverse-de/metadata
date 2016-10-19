(ns metadata.events
  (:require [clojure.tools.logging :as log]
            [metadata.util.config :as config]
            [metadata.amqp :as amqp]
            [langohr.basic :as lb])
  (:import [org.cyverse.events.ping PingMessages$Pong]
           [com.google.protobuf.util JsonFormat]))

(defn exchange-config
  []
  {:name        (config/exchange-name)
   :durable     (config/exchange-durable?)
   :auto-delete (config/exchange-auto-delete?)})

(defn queue-config
  []
  {:name        (config/queue-name)
   :durable     (config/queue-durable?)
   :auto-delete (config/queue-auto-delete?)})

(defn ping-handler
  [channel {:keys [delivery-tag routing-key]} msg]
  (lb/ack channel delivery-tag)
  (log/info (format "[events/ping-handler] [%s] [%s]" routing-key (String. msg)))
  (amqp/publish channel
                "events.metadata.pong"
                (.print (JsonFormat/printer)
                        (.. (PingMessages$Pong/newBuilder)
                            (setPongFrom "metadata")
                            (build)))
                {:content-type "application/json"}))
