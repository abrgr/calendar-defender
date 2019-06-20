FROM clojure:openjdk-8-lein-2.9.1-alpine

RUN apk -v --no-cache add \
        less \
        bash \
        openssh \
        curl \
        git

ENTRYPOINT ["/runner"]

VOLUME /root/.aws

