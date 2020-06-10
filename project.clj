(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/metadata "2.16.0-SNAPSHOT"
  :description "The REST API for the Discovery Environment Metadata services."
  :url "https://github.com/cyverse-de/metadata"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [net.sourceforge.owlapi/owlapi-api "3.5.0"]
                 [net.sourceforge.owlapi/owlapi-apibinding "3.4.10"]
                 [net.sourceforge.owlapi/owlapi-reasoner "3.3"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.8.1"]
                 [org.clojure/data.csv "0.1.4"]

                 ;; Langohr version 5.0.0 removes langohr.basic/blocking-subscribe.
                 [com.novemberain/langohr "4.2.0"]

                 [javax.servlet/servlet-api "2.5"]
                 [org.cyverse/clojure-commons "3.0.3"]
                 [org.cyverse/common-cfg "2.8.1"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/common-swagger-api "3.0.2"]
                 [org.cyverse/kameleon "3.0.3"]
                 [org.cyverse/service-logging "2.8.0"]
                 [org.cyverse/event-messages "0.0.1"]
                 [ring/ring-jetty-adapter "1.6.0"]
                 [sanitize-filename "0.1.0"]
                 [slingshot "0.12.2"]]
  :main ^:skip-aot metadata.core
  :uberjar-name "metadata-standalone.jar"
  :profiles {:dev     {:resource-paths ["conf/test"]}
             ;; compojure-api route macros should not be AOT compiled,
             ;; so that schema enum values can be loaded from the db
             :uberjar {:aot [#"metadata.(?!routes).*"]}}
  :plugins [[test2junit "1.2.2"]
            [jonase/eastwood "0.3.5"]]
  :eastwood {:exclude-namespaces [metadata.routes.schemas.template
                                  metadata.routes.schemas.permanent-id-requests
                                  metadata.routes.templates
                                  metadata.routes.permanent-id-requests
                                  metadata.routes
                                  :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml"])
