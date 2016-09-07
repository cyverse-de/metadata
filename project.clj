(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/metadata "2.8.1-SNAPSHOT"
  :description "The REST API for the Discovery Environment Metadata services."
  :url "https://github.com/cyverse-de/metadata"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [net.sourceforge.owlapi/owlapi-api "3.5.0"]
                 [net.sourceforge.owlapi/owlapi-apibinding "3.4.10"]
                 [net.sourceforge.owlapi/owlapi-reasoner "3.3"]
                 [metosin/compojure-api "1.1.8"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.6.3"]
                 [org.clojure/data.csv "0.1.3"]
                 [com.novemberain/langohr "3.5.1"]
                 [org.cyverse/clojure-commons "2.8.1-SNAPSHOT"]
                 [org.cyverse/common-cfg "2.8.0"]
                 [org.cyverse/common-cli "2.8.0"]
                 [org.cyverse/common-swagger-api "2.8.1-SNAPSHOT"]
                 [org.cyverse/kameleon "2.8.0"]
                 [org.cyverse/service-logging "2.8.0"]
                 [sanitize-filename "0.1.0"]
                 [slingshot "0.12.2"]]
  :main metadata.core
  :ring {:handler metadata.core/dev-handler
         :init    metadata.core/init-service
         :port    60000}
  :uberjar-name "metadata-standalone.jar"
  :profiles {:dev     {:plugins        [[lein-ring "0.9.6"]]
                       :resource-paths ["conf/test"]}
             ;; compojure-api route macros should not be AOT compiled:
             ;; https://github.com/metosin/compojure-api/issues/135#issuecomment-121388539
             ;; https://github.com/metosin/compojure-api/issues/102
             :uberjar {:aot [#"metadata.(?!routes).*"]}}
  :plugins [[test2junit "1.1.3"]
            [jonase/eastwood "0.2.3"]]
  :eastwood {:exclude-namespaces [metadata.routes.schemas.template
                                  metadata.routes.schemas.permanent-id-requests
                                  metadata.routes.templates
                                  metadata.routes.permanent-id-requests
                                  metadata.routes
                                  :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml"])
