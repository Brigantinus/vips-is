#!/bin/sh
# Generates a self-signed internal CA plus server/client certificates for mTLS
# between ImageServer and CacheServer.
#
# Runs once: if /certs/ca.crt already exists the script exits immediately so
# existing certificates survive container restarts.
#
# Output files (all written to /certs):
#   ca.crt / ca.key            – internal CA (never leaves the certs volume)
#   cacheserver.crt / .key     – server cert presented by CacheServer
#   imageserver.crt / .key     – client cert presented by ImageServer
set -e

CERTS=/certs

if [ -f "$CERTS/ca.crt" ]; then
    echo "Certificates already present, skipping generation."
    exit 0
fi

echo "Installing openssl..."
apk add --no-cache openssl > /dev/null 2>&1

echo "Generating internal CA..."
openssl genrsa -out "$CERTS/ca.key" 4096
openssl req -new -x509 -key "$CERTS/ca.key" -out "$CERTS/ca.crt" -days 3650 \
    -subj "/CN=vips-is-internal-ca/O=vips-is"

# CacheServer: server + client auth (server auth so imageserver can verify it;
# client auth so cacheserver can be verified if the roles are ever reversed)
echo "Generating cacheserver certificate..."
openssl genrsa -out "$CERTS/cacheserver.key" 2048

openssl req -new -key "$CERTS/cacheserver.key" -out /tmp/cacheserver.csr \
    -subj "/CN=cacheserver/O=vips-is"

cat > /tmp/cacheserver-ext.cnf <<'EOF'
subjectAltName=DNS:cacheserver,DNS:localhost
extendedKeyUsage=serverAuth,clientAuth
EOF

openssl x509 -req -in /tmp/cacheserver.csr \
    -CA "$CERTS/ca.crt" -CAkey "$CERTS/ca.key" -CAcreateserial \
    -out "$CERTS/cacheserver.crt" -days 365 \
    -extfile /tmp/cacheserver-ext.cnf

# ImageServer: client auth only (authenticates itself to CacheServer)
echo "Generating imageserver certificate..."
openssl genrsa -out "$CERTS/imageserver.key" 2048

openssl req -new -key "$CERTS/imageserver.key" -out /tmp/imageserver.csr \
    -subj "/CN=imageserver/O=vips-is"

cat > /tmp/imageserver-ext.cnf <<'EOF'
subjectAltName=DNS:imageserver
extendedKeyUsage=clientAuth
EOF

openssl x509 -req -in /tmp/imageserver.csr \
    -CA "$CERTS/ca.crt" -CAkey "$CERTS/ca.key" -CAcreateserial \
    -out "$CERTS/imageserver.crt" -days 365 \
    -extfile /tmp/imageserver-ext.cnf

chmod 644 "$CERTS"/*.crt "$CERTS"/*.key

echo "Done. Generated:"
ls -la "$CERTS"
