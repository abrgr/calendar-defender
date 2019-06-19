FROM mesosphere/aws-cli

RUN apk --no-cache add bash openssh curl && \
    curl https://docs.datomic.com/cloud/files/datomic-socks-proxy > /datomic-socks-proxy && \
    chmod 755 /datomic-socks-proxy && \
    printf "Host *\n  StrictHostKeyChecking no\n  UserKnownHostsFile=/dev/null" > /etc/ssh/ssh_config


ENTRYPOINT ["/datomic-socks-proxy"]
