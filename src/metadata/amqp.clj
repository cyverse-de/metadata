(ns metadata.amqp
  (:require [metadata.util.config :as config]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.basic :as lb]
            [cheshire.core :as json]))

(defn publish-metadata-update
  [user id]
  (let [conn (rmq/connect {:uri (config/amqp-uri)})
        ch (lch/open conn)
        ename (config/exchange-name)]
    (le/declare ch ename (config/exchange-type) {:durable true})
    (lb/publish ch ename "metadata.update" (json/encode {:entity id :author user}) {:content-type "application/json" :persistent 1})
    (rmq/close ch)
    (rmq/close conn)))
