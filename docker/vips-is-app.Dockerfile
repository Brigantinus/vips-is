FROM ghcr.io/brigantinus/vips-is/vips-is-base:latest

WORKDIR /deployments

COPY --chown=185 ../target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 ../target/quarkus-app/*.jar /deployments/
COPY --chown=185 ../target/quarkus-app/app/ /deployments/app/
COPY --chown=185 ../target/quarkus-app/quarkus/ /deployments/quarkus/

COPY --chown=185:185 --chmod=755 docker/entrypoint.sh /deployments/entrypoint.sh

USER 185

EXPOSE 8080

ENTRYPOINT ["/deployments/entrypoint.sh"]
