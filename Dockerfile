FROM discoenv/clojure-base:master

ENV CONF_TEMPLATE=/usr/src/app/metadata.properties.tmpl
ENV CONF_FILENAME=metadata.properties
ENV PROGRAM=metadata

VOLUME ["/etc/iplant/de"]

COPY project.clj /usr/src/app/
RUN lein deps

COPY conf/main/logback.xml /usr/src/app/
COPY . /usr/src/app

RUN lein uberjar && \
    cp target/metadata-standalone.jar .

RUN ln -s "/usr/bin/java" "/bin/metadata"

ENTRYPOINT ["run-service", "-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml", "-cp", ".:metadata-standalone.jar:/", "metadata.core"]

ARG git_commit=unknown
ARG version=unknown
ARG descriptive_version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"
LABEL org.cyverse.descriptive-version="$descriptive_version"
LABEL org.label-schema.vcs-ref="$git_commit"
LABEL org.label-schema.vcs-url="https://github.com/cyverse-de/metadata"
LABEL org.label-schema.version="$descriptive_version"
