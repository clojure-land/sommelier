FROM java:8

LABEL maintainer=freid

ARG VERSION
ARG BUILD_TIME
ARG GHASH

ENV VERSION $VERSION
ENV BUILD_TIME $BUILD_TIME
ENV GHASH $GHASH

COPY target /opt/s2s-api/bin
COPY entrypoint.sh /opt/s2s-api/bin/entrypoint.sh

RUN chmod 500 /opt/s2s-api/bin/entrypoint.sh

WORKDIR /opt/s2s-api/bin

EXPOSE 3000

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]