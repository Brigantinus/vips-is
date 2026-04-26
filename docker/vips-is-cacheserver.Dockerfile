# Build context: project root
# docker build -f docker/vips-is-cacheserver.Dockerfile -t ghcr.io/brigantinus/vips-is/cacheserver:latest .
#
# No libvips needed — the cacheserver is a pure Java service.

FROM eclipse-temurin:25-jre-noble

RUN apt-get update && apt-get install -y --no-install-recommends \
    libjemalloc2 \
    && rm -rf /var/lib/apt/lists/*

# jemalloc reduces heap fragmentation for the large off-heap Arena allocations
ENV LD_PRELOAD="/usr/lib/x86_64-linux-gnu/libjemalloc.so.2"

WORKDIR /deployments

COPY --chown=185 cacheserver/target/quarkus-app/lib/     /deployments/lib/
COPY --chown=185 cacheserver/target/quarkus-app/*.jar    /deployments/
COPY --chown=185 cacheserver/target/quarkus-app/app/     /deployments/app/
COPY --chown=185 cacheserver/target/quarkus-app/quarkus/ /deployments/quarkus/

COPY --chown=185:185 --chmod=755 docker/cacheserver-entrypoint.sh /deployments/entrypoint.sh

USER 185

# gRPC (mTLS)
EXPOSE 9000
# Quarkus management (metrics, health)
EXPOSE 9001

ENTRYPOINT ["/deployments/entrypoint.sh"]
