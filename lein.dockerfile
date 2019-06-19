FROM clojure:openjdk-8-lein-2.9.1-alpine

RUN apk -v --no-cache add \
        python \
        py-pip \
        groff \
        less \
        mailcap \
        bash \
        openssh \
        curl \
      && \
      pip install --upgrade awscli==1.14.5 s3cmd==2.0.1 python-magic && \
      apk -v --purge del py-pip && \
      curl https://docs.datomic.com/cloud/files/datomic-socks-proxy > /datomic-socks-proxy && \
      chmod 755 /datomic-socks-proxy && \
      printf "Host *\n  StrictHostKeyChecking no\n  UserKnownHostsFile=/dev/null" > /etc/ssh/ssh_config && \
      printf '#!/bin/bash\n/datomic-socks-proxy "$1" &\nlein "${@:2}"' > /runner

ENTRYPOINT ["jshell", "lein"]

VOLUME /root/.aws

