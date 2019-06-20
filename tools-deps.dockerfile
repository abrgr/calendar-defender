FROM clojure:openjdk-8-tools-deps-1.10.0.442-alpine

RUN apk -v --no-cache add \
        python \
        py-pip \
        groff \
        less \
        mailcap \
        bash \
        openssh \
        git \
      && \
      pip install --upgrade awscli==1.14.5 s3cmd==2.0.1 python-magic && \
      apk -v --purge del py-pip

VOLUME /root/.aws
