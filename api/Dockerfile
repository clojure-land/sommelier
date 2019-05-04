FROM java:8

LABEL maintainer=freid

ARG VERSION
ARG BUILD_TIME

ENV VERSION $VERSION
ENV BUILD_TIME $BUILD_TIME

COPY target /opt/sommelier-api/bin
COPY entrypoint.sh /opt/sommelier-api/bin/entrypoint.sh

RUN chmod 500 /opt/sommelier-api/bin/entrypoint.sh

WORKDIR /opt/sommelier-api/bin

EXPOSE 3000

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]