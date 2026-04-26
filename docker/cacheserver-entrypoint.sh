#!/bin/sh

echo "--- CacheServer Startup ---"

exec java \
    -Dquarkus.http.host=0.0.0.0 \
    -jar /deployments/quarkus-run.jar
