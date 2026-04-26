# Build context: project root
# docker build -f docker/vips-is-imageserver.Dockerfile -t ghcr.io/brigantinus/vips-is/imageserver:latest .

FROM ghcr.io/brigantinus/vips-is/vips-is-base:latest

WORKDIR /deployments

COPY --chown=185 imageserver/target/quarkus-app/lib/     /deployments/lib/
COPY --chown=185 imageserver/target/quarkus-app/*.jar    /deployments/
COPY --chown=185 imageserver/target/quarkus-app/app/     /deployments/app/
COPY --chown=185 imageserver/target/quarkus-app/quarkus/ /deployments/quarkus/

COPY --chown=185:185 --chmod=755 docker/imageserver-entrypoint.sh /deployments/entrypoint.sh

USER 185

# HTTP API
EXPOSE 8080
# Quarkus management (metrics, health)
EXPOSE 9000

ENTRYPOINT ["/deployments/entrypoint.sh"]
