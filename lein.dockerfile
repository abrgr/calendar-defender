FROM clojure:openjdk-8-lein-2.9.1-alpine

RUN apk -v --no-cache add \
        python \
        py-pip \
        groff \
        less \
        mailcap \
      && \
      pip install --upgrade awscli==1.14.5 s3cmd==2.0.1 python-magic && \
      apk -v --purge del py-pip

VOLUME /root/.aws

