(ns metadata.amqp
  (:require [metadata.util.config :as config]
            [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.basic :as lb]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [cheshire.core :as json]))

(def local-channel (ref nil))

(defn channel
  ([]
   (deref local-channel))
  ([val]
   (dosync (ref-set local-channel val))))

(defn- declare-exchange
  [channel {exchange-name :name :as exchange-cfg}]
  (le/topic channel exchange-name exchange-cfg))

(defn exchange-config
  []
  {:name        (config/exchange-name)
   :durable     (config/exchange-durable?)
   :auto-delete (config/exchange-auto-delete?)})

(defn connect
  [exchange-cfg]
  (let [channel (lch/open (rmq/connect {:uri (config/amqp-uri)}))]
    (log/info (format "[amqp/connect] [%s]" (config/amqp-uri)))
    (declare-exchange channel exchange-cfg)
    channel))

(defn publish
  [channel routing-key msg msg-meta]
  (lb/publish channel (config/exchange-name) routing-key msg msg-meta))

(defn publish-metadata-update
  [user id]
  (publish (channel)
           "metadata.update"
           (json/encode {:entity id
                         :author user})
           {:content-type "application/json"
            :persistent   1}))
