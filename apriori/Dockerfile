FROM java:8

LABEL maintainer=freid

COPY target /opt/sommelier-apriori/bin
COPY entrypoint.sh /opt/sommelier-apriori/bin/entrypoint.sh

RUN chmod 500 /opt/sommelier-apriori/bin/entrypoint.sh

WORKDIR /opt/sommelier-apriori/bin

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]