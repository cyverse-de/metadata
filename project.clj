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
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [net.sourceforge.owlapi/owlapi-api "5.5.0"]
                 [net.sourceforge.owlapi/owlapi-apibinding "5.5.0"]
                 [net.sourceforge.owlapi/owlapi-reasoner "3.3"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.13.0"]
                 [org.clojure/data.csv "1.1.0"]

                 ;; Langohr version 5.0.0 removes langohr.basic/blocking-subscribe.
                 [com.novemberain/langohr "4.2.0"]

                 [javax.servlet/servlet-api "2.5"]
                 [org.cyverse/clojure-commons "3.0.9"]
                 [org.cyverse/common-cfg "2.8.3"]
                 [org.cyverse/common-cli "2.8.2"]
                 [org.cyverse/common-swagger-api "3.4.5"]
                 [org.cyverse/kameleon "3.0.10"]
                 [org.cyverse/service-logging "2.8.4"]
                 [org.cyverse/event-messages "0.0.1"]
                 [org.cyverse/otel "0.2.6"]
                 [ring/ring-jetty-adapter "1.12.2"]
                 [sanitize-filename "0.1.0"]
                 [slingshot "0.12.2"]]
  :main ^:skip-aot metadata.core
  :uberjar-name "metadata-standalone.jar"
  :profiles {:dev     {:resource-paths ["conf/test"]
                       :jvm-opts ["-Dotel.javaagent.enabled=false"]}
             ;; compojure-api route macros should not be AOT compiled,
             ;; so that schema enum values can be loaded from the db
             :uberjar {:aot [#"metadata.(?!routes).*"]
                       :jvm-opts ["-Dotel.javaagent.enabled=false"]}}
  :plugins [[jonase/eastwood "1.4.3"]
            [lein-ancient "0.7.0"]
            [test2junit "1.4.4"]]
  :eastwood {:exclude-namespaces [metadata.routes.schemas.template
                                  metadata.routes.schemas.permanent-id-requests
                                  metadata.routes.templates
                                  metadata.routes.permanent-id-requests
                                  metadata.routes
                                  :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml" "-javaagent:./opentelemetry-javaagent.jar" "-Dotel.resource.attributes=service.name=metadata"])
