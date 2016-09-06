FROM clojure:alpine

RUN apk add --update git && \
    rm -rf /var/cache/apk

VOLUME ["/etc/iplant/de"]

ARG git_commit=unknown
ARG version=unknown
LABEL org.iplantc.de.metadata.git-ref="$git_commit" \
      org.iplantc.de.metadata.version="$version"

COPY . /usr/src/app
COPY conf/main/logback.xml /usr/src/app/logback.xml

WORKDIR /usr/src/app

RUN lein uberjar && \
    cp target/metadata-standalone.jar .

RUN ln -s "/usr/bin/java" "/bin/metadata"

ENTRYPOINT ["metadata", "-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml", "-cp", ".:metadata-standalone.jar:/", "metadata.core"]
CMD ["--help"]
