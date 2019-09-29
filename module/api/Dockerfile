FROM java:8

LABEL maintainer=freid

COPY target /opt/sommelier-api/bin
COPY entrypoint.sh /opt/sommelier-api/bin/entrypoint.sh

RUN chmod 500 /opt/sommelier-api/bin/entrypoint.sh

ARG VERSION

ENV VERSION $VERSION

WORKDIR /opt/sommelier-api/bin

EXPOSE 3000

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]