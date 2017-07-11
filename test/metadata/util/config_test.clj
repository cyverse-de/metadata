(ns metadata.util.config-test
  (:use [clojure.test])
  (:require [metadata.util.config :as config]))

(deftest test-config-defaults
  (testing "default configuration settings"
    (require 'metadata.util.config :reload)
    (config/load-config-from-file "conf/test/empty.properties")
    (is (= (config/listen-port) 60000))
    (is (= (config/environment-name) "docker-compose"))
    (is (= (config/db-subprotocol) "pgsql"))
    (is (= (config/db-host) "dedb"))
    (is (= (config/db-port) "5432"))
    (is (= (config/db-name) "metadata"))
    (is (= (config/db-user) "de"))
    (is (= (config/db-password) "notprod"))
    (is (= (config/amqp-uri) "amqp://guest:guestPW@localhost:5672"))
    (is (= (config/exchange-name) "de"))
    (is (= (config/exchange-type) "topic"))
    (is (config/exchange-durable?))
    (is (not (config/exchange-auto-delete?)))
    (is (= (config/queue-name) "events.metadata.queue"))
    (is (config/queue-durable?))
    (is (not (config/queue-auto-delete?)))))
