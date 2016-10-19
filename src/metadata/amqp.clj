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

(defn- declare-queue
  [channel {exchange-name :name} queue-cfg topics]
  (lq/declare channel (:name queue-cfg) (assoc queue-cfg :exclusive false))
  (doseq [key topics]
    (lq/bind channel (:name queue-cfg) exchange-name {:routing-key key})))

(defn- declare-exchange
  [channel {exchange-name :name :as exchange-cfg}]
  (le/topic channel exchange-name exchange-cfg))

(defn- message-router
  [handlers channel {:keys [delivery-tag routing-key] :as metadata} msg]
  (let [handler (get handlers routing-key)]
    (if-not (nil? handler)
      (handler channel metadata msg)
      (log/error (format "[amqp/message-router] [%s] [%s] unroutable" routing-key (String. msg))))))

(defn connect
  [exchange-cfg queue-cfg routing-keys]
  (let [channel (lch/open (rmq/connect {:uri (config/amqp-uri)}))]
    (log/info (format "[amqp/connect] [%s]" (config/amqp-uri)))
    (declare-exchange channel exchange-cfg)
    (declare-queue channel exchange-cfg queue-cfg routing-keys)
    channel))

(defn subscribe
  [channel queue-name handlers]
  (lc/blocking-subscribe channel queue-name (partial message-router handlers)))

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
