FROM openjdk:21-jdk as stage0
WORKDIR /opt/docker
COPY target/docker/stage/2/opt /2/opt
COPY target/docker/stage/4/opt /4/opt
USER root
RUN ["chmod", "-R", "u=rX,g=rX", "/2/opt/docker"]
RUN ["chmod", "-R", "u=rX,g=rX", "/4/opt/docker"]
RUN ["chmod", "u+x,g+x", "/4/opt/docker/bin/passulo-server"]

FROM openjdk:21-jdk as mainstage

LABEL org.opencontainers.image.source https://github.com/passulo/Passulo-Server
LABEL org.opencontainers.image.url http://www.passulo.com
LABEL name="Passulo Server"
LABEL email="mail@passulo.com"

USER root

RUN id -u passulo 1>/dev/null 2>&1 || (( getent group 0 1>/dev/null 2>&1 || ( type groupadd 1>/dev/null 2>&1 && groupadd -g 0 root || addgroup -g 0 -S root )) && ( type useradd 1>/dev/null 2>&1 && useradd --system --create-home --uid 1001 --gid 0 passulo || adduser -S -u 1001 -G root passulo ))

WORKDIR /opt/docker
COPY --from=stage0 --chown=passulo:root /2/opt/docker /opt/docker
COPY --from=stage0 --chown=passulo:root /4/opt/docker /opt/docker

EXPOSE 8080
USER 1001:0
ENTRYPOINT ["/opt/docker/bin/passulo-server"]
CMD []
