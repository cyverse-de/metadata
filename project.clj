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
  ;; Fail the build on a new dependency conflict rather than printing a warning
  ;; nobody reads. If this ever aborts, re-derive :managed-dependencies below.
  :pedantic? :abort
  ;; XXX(cider-leak): several published org.cyverse artifacts (clojure-commons
  ;; 3.0.12, common-cfg 2.8.3, common-cli 2.8.2, kameleon 3.0.10 and
  ;; service-logging 2.8.5) carry cider/cider-nrepl 0.49.1 at compile scope in
  ;; their poms, so a REPL tool ends up on the runtime classpath and in the
  ;; uberjar. Their project.clj files no longer declare it; the fix simply has
  ;; not been released yet. Drop this once those artifacts are re-released.
  :exclusions [cider/cider-nrepl]
  ;; Every entry here except the jackson-* group records a version Leiningen
  ;; already resolves. Those are read off the resolved classpath rather than
  ;; copied from lein's "Consider using these :managed-dependencies" hint --
  ;; the hint names the version that LOST the conflict, so pasting it would be
  ;; a silent upgrade. Pinning the resolved version records the existing choice
  ;; as deliberate and leaves the runtime classpath unchanged.
  ;;
  ;; The jackson-* entries are the deliberate exception. Those artifacts must
  ;; move together -- mixing minors across core/databind/annotations surfaces as
  ;; NoSuchMethodError at runtime rather than as a resolution failure -- and the
  ;; cheshire 6 upgrade left them spread across 2.13/2.18/2.21. cheshire 6.2.0
  ;; brings core/cbor/smile at 2.21.1, so databind is aligned to match and
  ;; annotations pinned to the 2.21 that databind 2.21.1 declares (annotations
  ;; uses a minor-only version scheme). Without the annotations pin,
  ;; owlapi-rio's transitive 2.13.5 wins by nearest-wins.
  ;;
  ;; Re-derive this block whenever a dependency that arbitrates it is bumped.
  :managed-dependencies [[clj-http "3.13.0"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.21"]
                         [com.fasterxml.jackson.core/jackson-core "2.21.1"]
                         [com.fasterxml.jackson.core/jackson-databind "2.21.1"]
                         [com.google.errorprone/error_prone_annotations "2.21.1"]
                         [commons-codec "1.16.1"]
                         [org.checkerframework/checker-qual "3.37.0"]
                         [prismatic/schema "1.1.12"]]
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [net.sourceforge.owlapi/owlapi-api "5.5.1"
                  :exclusions [[org.slf4j/slf4j-api] [commons-io]]]
                 [net.sourceforge.owlapi/owlapi-apibinding "5.5.1"
                  :exclusions [[org.slf4j/slf4j-api] [commons-io]]]
                 [net.sourceforge.owlapi/owlapi-reasoner "3.3"
                  :exclusions [[org.slf4j/slf4j-api] [commons-io]]]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "6.2.0"]
                 [org.clojure/data.csv "1.1.1"]
                 [com.novemberain/langohr "5.6.0"
                  :exclusions [org.slf4j/slf4j-api]]
                 [javax.servlet/servlet-api "2.5"]
                 [org.cyverse/clojure-commons "3.0.12"]
                 [org.cyverse/common-cfg "2.8.3"]
                 [org.cyverse/common-cli "2.8.2"]
                 [org.cyverse/common-swagger-api "3.4.22"]
                 [org.cyverse/kameleon "3.0.10"]
                 [org.cyverse/service-logging "2.8.5"]
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
            [lein-ancient "1.0.0"]
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
