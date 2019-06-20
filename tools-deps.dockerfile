FROM clojure:openjdk-8-tools-deps-1.10.0.442-alpine

RUN apk -v --no-cache add \
        python \
        py-pip \
        groff \
        less \
        mailcap \
        bash \
        openssh \
        curl \
        git \
        readline-dev \
      && \
      pip install --upgrade awscli==1.14.5 s3cmd==2.0.1 python-magic && \
      apk -v --purge del py-pip && \
      curl https://docs.datomic.com/cloud/files/datomic-socks-proxy > /datomic-socks-proxy && \
      chmod 755 /datomic-socks-proxy && \
      printf "Host *\n  StrictHostKeyChecking no\n  UserKnownHostsFile=/dev/null" > /etc/ssh/ssh_config && \
      printf '#!/bin/bash\n/datomic-socks-proxy --port 0.0.0.0:8182 "$1" >/dev/null 2>&1 &\nexec clojure -A:dev' > /repl && \
      chmod 755 /repl

VOLUME /root/.aws
