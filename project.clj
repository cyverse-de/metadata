(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/metadata "3.0.1-SNAPSHOT"
  :description "The REST API for the Discovery Environment Metadata services."
  :url "https://github.com/cyverse-de/metadata"
  :license {:name "BSD Standard License"
            :url "https://cyverse.org/license"}
  :manifest {"Git-Ref" ~(git-ref)}
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [net.sourceforge.owlapi/owlapi-api "5.5.0"
                  :exclusions [[org.slf4j/slf4j-api] [commons-io]]]
                 [net.sourceforge.owlapi/owlapi-apibinding "5.5.0"
                  :exclusions [[org.slf4j/slf4j-api] [commons-io]]]
                 [net.sourceforge.owlapi/owlapi-reasoner "3.3"
                  :exclusions [[org.slf4j/slf4j-api] [commons-io]]]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.13.0"]
                 [org.clojure/data.csv "1.1.0"]
                 [com.novemberain/langohr "5.4.0"
                  :exclusions [org.slf4j/slf4j-api]]
                 [javax.servlet/servlet-api "2.5"]
                 [org.cyverse/clojure-commons "3.0.9"]
                 [org.cyverse/common-cfg "2.8.3"]
                 [org.cyverse/common-cli "2.8.2"]
                 [org.cyverse/common-swagger-api "3.4.5"]
                 [org.cyverse/kameleon "3.0.10"]
                 [org.cyverse/service-logging "2.8.4"]
                 [ring/ring-core "1.12.2"]
                 [ring/ring-jetty-adapter "1.12.2"]
                 [sanitize-filename "0.1.0"]
                 [slingshot "0.12.2"]]
  :main ^:skip-aot metadata.core
  :uberjar-name "metadata-standalone.jar"
  :profiles {:dev     {:resource-paths ["conf/test"]}
             ;; compojure-api route macros should not be AOT compiled,
             ;; so that schema enum values can be loaded from the db
             :uberjar {:aot [#"metadata.(?!routes).*"]}}
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
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml"])
