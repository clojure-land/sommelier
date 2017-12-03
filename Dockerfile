FROM java:8

LABEL maintainer=freid

ARG VERSION
ARG BUILD_TIME
ARG GHASH

ENV VERSION $VERSION
ENV BUILD_TIME $BUILD_TIME
ENV GHASH $GHASH

COPY target /opt/apriori/bin
COPY entrypoint.sh /opt/apriori/bin/entrypoint.sh
COPY apriori.sh /usr/bin/apriori

RUN chmod 500 /opt/apriori/bin/entrypoint.sh
RUN chmod +x /usr/bin/apriori

WORKDIR /opt/apriori/bin

EXPOSE 3000

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]