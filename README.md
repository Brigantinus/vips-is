# vips-is — IIIF Image Server

An [IIIF Image API](https://iiif.io/api/image/) server (versions 2.1 and 3.0) built on [Quarkus](https://quarkus.io) and [libvips](https://www.libvips.org) via the [vips-ffm](https://github.com/lopcode/vips-ffm) Java binding. Images are processed on-demand, cached off-heap in a companion service, and served with IIIF-standard HTTP caching headers.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Modules](#modules)
  - [shared](#shared)
  - [imageserver](#imageserver)
  - [cacheserver](#cacheserver)
- [IIIF API](#iiif-api)
- [Image Processing with libvips](#image-processing-with-libvips)
- [Caching Architecture](#caching-architecture)
  - [Off-heap memory model](#off-heap-memory-model)
  - [Eviction policy](#eviction-policy)
  - [Cache sizing](#cache-sizing)
  - [gRPC transport and mTLS](#grpc-transport-and-mtls)
- [Storage Backends](#storage-backends)
- [Configuration Reference](#configuration-reference)
  - [ImageServer](#imageserver-configuration)
  - [CacheServer](#cacheserver-configuration)
- [Local Setup from Source](#local-setup-from-source)
- [Docker Compose](#docker-compose)
- [Kubernetes](#kubernetes)

---

## Architecture Overview

```
  Internet
      │
      ▼
  Ingress  (consistent hash by IIIF identifier)
      │
  ┌───┴────────────────────┐
  │  ImageServer (×N pods) │  Quarkus · REST · libvips
  │  - parse IIIF request  │
  │  - apply crop/resize/  │
  │    rotate/quality      │
  └──────────┬─────────────┘
             │ gRPC / mTLS
             ▼
  ┌──────────────────────────────┐
  │  CacheServer (×1 pod)        │  Quarkus · gRPC
  │  - off-heap MemorySegment    │
  │  - FIFO + TTL eviction       │
  │  - original bytes + tiles    │
  └──────────┬───────────────────┘
             │
             ▼
       StorageBackend
       ├── Filesystem
       └── S3-compatible
```

**ImageServer** is the IIIF-facing component. It parses incoming requests, retrieves image bytes from the CacheServer, transforms them with libvips, caches the result back, and returns the response. It is stateless and scales horizontally.

**CacheServer** is a pure storage service. It loads raw image bytes from a configured backend (filesystem or S3), keeps them in off-heap memory, and serves subsequent requests without touching the backend again. It is stateful and runs as a single high-memory instance.

**Consistent-hash routing** at the ingress ensures that all tile requests for the same image identifier reach the same ImageServer pod. This keeps each pod's gRPC round-trip count low: on a warm derived-cache hit the tile is returned without re-running libvips at all.

---

## Modules

The project is a multi-module Maven build:

```
vips-is/
├── pom.xml          ← parent POM — dependency management only, no code
├── shared/          ← shared model, interfaces, proto definition
├── imageserver/     ← IIIF REST API + libvips image processing
└── cacheserver/     ← off-heap cache + storage backends + gRPC server
```

### shared

A plain Java library (no Quarkus runtime) consumed by both applications. Contains:

| Package | Contents |
|---|---|
| `service/image/model` | `ImageRequest`, `ImageInfo`, `RegionInfo`, `SizeInfo`, `RotationInfo`, `ImageFormat`, `QualityMode`, `Region`, `Size`, `Tile`, … |
| `service/image/exceptions` | `ImageProcessingException`, `InvalidParameterException` |
| `service/storage/exceptions` | `ImageNotFoundException`, `StorageException` |
| `service/cache` | `ImageCache` interface, `DerivedKey` record, `CacheStats` record, `ImageTooLargeForCacheException` |
| `grpc` | `ProtoMapper` — converts between domain objects and proto-generated message types |
| `src/main/proto` | `cache.proto` — single source of truth for the gRPC contract |

The proto file is compiled once inside `shared`. Both `imageserver` and `cacheserver` consume the generated stubs through the `shared` dependency — no duplication, one file governs both sides of the gRPC channel.

### imageserver

The IIIF-facing Quarkus application. Runs on port **8080** (HTTP) and port **9000** (management/metrics).

Key classes:

| Class | Responsibility |
|---|---|
| `ImageResourceV3` / `ImageResourceV2` | JAX-RS endpoints for IIIF v3 / v2 |
| `ImageServiceImpl` | Orchestrates cache lookups and libvips processing |
| `VipsProcessor` | Wraps vips-ffm: crop, resize, rotate, quality, format conversion |
| `ImageInfoCalculator` | Computes tile pyramid metadata from image dimensions |
| `RegionCalculator` / `SizeCalculator` | Translate IIIF parameter strings to pixel coordinates |
| `RemoteImageCache` | `ImageCache` implementation — translates calls to gRPC towards CacheServer |
| `ImageClientCacheFilter` | Adds `Cache-Control` / `Expires` headers to every image response |

### cacheserver

The internal Quarkus application. Runs on port **9000** (gRPC / mTLS) and port **9001** (management/metrics).

Key classes:

| Class | Responsibility |
|---|---|
| `CacheGrpcService` | Implements the gRPC `CacheService` — handles all RPCs |
| `ImageCacheImpl` | FIFO eviction, TTL sweep, read/write locking |
| `CacheEntry` | One entry per identifier: off-heap `Arena` + `MemorySegment` map |
| `CacheSizeCalculator` | Auto-sizes the cache from cgroup / system memory at startup |
| `FilesystemStorageBackend` | Loads image bytes from a local directory |
| `S3StorageBackend` | Loads image bytes from any S3-compatible object store |
| `CacheAdminResource` | REST `DELETE /cache/{identifier}` for urgent manual eviction |

---

## IIIF API

Both IIIF Image API v2.1 and v3.0 are supported on the same server instance.

### Endpoints

```
GET  /iiif/v3/image/{identifier}/info.json
GET  /iiif/v3/image/{identifier}/{region}/{size}/{rotation}/{quality}.{format}
HEAD /iiif/v3/image/{identifier}/info.json   (existence check)

GET  /iiif/v2/image/{identifier}/info.json
GET  /iiif/v2/image/{identifier}/{region}/{size}/{rotation}/{quality}.{format}
HEAD /iiif/v2/image/{identifier}/info.json
```

The identifier must be URL-encoded if it contains slashes or special characters (e.g. `archive%2Fpainting.tif`).

### Parameters

**Region** — portion of the source image to extract:

| Value | Meaning |
|---|---|
| `full` | Entire image |
| `square` | Largest centred square |
| `x,y,w,h` | Pixel rectangle |
| `pct:x,y,w,h` | Percentage rectangle |

**Size** — dimensions of the output image:

| Value | Meaning |
|---|---|
| `max` | Maximum allowed size (respects `image-server.max-width/height`) |
| `w,` | Width only; height proportional |
| `,h` | Height only; width proportional |
| `w,h` | Exact width and height (may distort) |
| `!w,h` | Fit within box, maintain aspect ratio |
| `pct:n` | Scale to n % of original |
| `^max` / `^w,h` | Allow upscaling beyond source dimensions |

**Rotation** — degrees clockwise; prefix `!` for horizontal mirror before rotation. Supports `0`, `90`, `180`, `270`, and arbitrary float values for non-orthogonal rotation.

**Quality:**

| Value | Effect |
|---|---|
| `default` / `color` | No colour transformation |
| `gray` | Convert to greyscale |
| `bitonal` | Two-level black and white |

**Format:**

| Extension | MIME type |
|---|---|
| `jpg` | `image/jpeg` |
| `png` | `image/png` |
| `webp` | `image/webp` |
| `gif` | `image/gif` |
| `tif` | `image/tiff` |
| `jp2` | `image/jp2` |

### Example requests

```bash
# Image metadata
GET /iiif/v3/image/archive%2Fpainting.tif/info.json

# 256×256 tile at offset (256, 0) — typical IIIF viewer tile request
GET /iiif/v3/image/archive%2Fpainting.tif/256,0,256,256/256,/0/default.jpg

# Full image scaled to 800 px wide, greyscale PNG
GET /iiif/v3/image/archive%2Fpainting.tif/full/800,/0/gray.png

# Mirrored and rotated 90°
GET /iiif/v3/image/archive%2Fpainting.tif/full/max/!90/default.jpg
```

---

## Image Processing with libvips

[libvips](https://www.libvips.org) is a demand-driven image processing library that reads only the pixels it needs. For tiled IIIF serving this is critical: extracting a 256×256 tile from a 1 GB multi-resolution TIFF reads just that tile from disk, not the entire file.

The Java binding [vips-ffm](https://github.com/lopcode/vips-ffm) calls libvips via the Java Foreign Function & Memory (FFM) API — no JNI, no reflection, minimal overhead.

### Processing pipeline

Every tile request follows this pipeline inside `VipsProcessor`:

```
byte[] originalBytes  (from CacheServer)
    │
    └── VImage.newFromBytes(arena, bytes)   # decode into vips arena
            │
            ├── extractArea(x, y, w, h)     # crop to requested region
            ├── resize(scaleX, scaleY)       # scale to requested size
            ├── flip / rot                   # mirror + orthogonal rotation
            ├── rotate(degrees)              # arbitrary-angle rotation
            ├── colourspace conversion        # gray / bitonal quality modes
            │
            └── encode to byte[]
                    ├── jpegsaveTarget / pngsaveTarget / …  (JPG, PNG, WebP, GIF)
                    └── tiffsaveBuffer / jp2ksaveBuffer     (TIFF, JPEG2000)
```

libvips executes the pipeline lazily — it builds an operation graph and reads only the pixels required to produce the output. For a 256×256 tile from a pyramidal TIFF, libvips reads from the nearest resolution level, avoiding full-image decodes entirely.

### Memory management

All vips objects are allocated inside a `java.lang.foreign.Arena`. When the arena is closed at the end of `Vips.run(...)`, all vips memory is released immediately, with no GC involvement. The JVM flag `--enable-native-access=ALL-UNNAMED` is required to use the FFM API.

The `--add-opens java.base/java.lang=ALL-UNNAMED` flag is also required by the vips-ffm binding internals and is included in the provided Dockerfiles and startup scripts.

### Supported source formats

Configured by `image-server.source-formats`. Default accepted extensions: `jpg`, `jpeg`, `jpe`, `jif`, `jfif`, `jfi`, `png`, `tif`, `tiff`, `gif`, `webp`.

TIFF and JPEG2000 are the most common archival formats in IIIF deployments. For pyramidal TIFFs, libvips reads from the nearest resolution level for each tile request, decoding only the required region rather than the full image.

---

## Caching Architecture

The cache exists because IIIF viewers fetch the same source image many times: once for `info.json`, then in dozens of tile requests across multiple zoom levels. Without caching every request re-reads the full source image from storage and re-decodes it.

### What is cached

| Item | Storage | Key | Lifetime |
|---|---|---|---|
| Original image bytes | Off-heap `MemorySegment` | identifier string | TTL (default 24 h) |
| `ImageInfo` (tile pyramid metadata) | On-heap Java object | identifier string | TTL |
| Derived tiles | Off-heap `MemorySegment` | `DerivedKey` record | TTL |

A `DerivedKey` encodes the complete set of IIIF parameters `(RegionInfo, SizeInfo, RotationInfo, QualityMode, ImageFormat)`. Two requests with identical parameters hit the same cached byte array.

### Off-heap memory model

Each `CacheEntry` owns one `Arena.ofShared()`. The original image bytes and every derived tile are `MemorySegment`s allocated within that arena.

```
CacheEntry "archive/painting.tif"
├── Arena (ofShared)                         ← owns all memory for this image
├── MemorySegment  originalSegment           ← raw source bytes, off-heap
├── ImageInfo      info                      ← IIIF metadata, on-heap
└── ConcurrentHashMap<DerivedKey, MemorySegment>   ← one entry per tile variant
```

**Why off-heap?** Archival source images are frequently hundreds of megabytes. Storing them on the GC heap causes stop-the-world pauses proportional to live heap size and limits the number of images that can be held simultaneously. Off-heap `MemorySegment`s are invisible to the GC; `-Xmx` can be kept small.

**Deterministic release.** When an entry is evicted, `arena.close()` immediately returns all memory for that entry to the OS — no GC cycle, no finaliser, no unpredictable pause.

**Use-after-close protection.** A reference count (`AtomicInteger`) prevents a race between a reader thread and a concurrent eviction:

```
refCount starts at 1   (the cache itself holds the initial reference)

reader arrives:   acquire() → refCount = 2
eviction fires:   close()   → refCount = 1  (arena still open)
reader finishes:  close()   → refCount = 0  → arena.close()
```

`arena.close()` fires only when every reference has been released; no thread ever accesses a closed segment.

### Eviction policy

**FIFO + total size limit.** The backing store is a `LinkedHashMap` in insertion order. When the cache exceeds `maxBytes`, entries are removed from the head (oldest first) until there is enough space. Eviction is whole-entry: the original bytes and all derived tiles for an identifier are released together as a unit, ensuring consistency.

**TTL sweep.** A scheduled task runs every hour and removes entries whose `loadedAt` timestamp is older than `cache.ttl-hours` (default 24 h). The next access after expiry triggers a fresh load from the storage backend.

**Hard size limit.** If a single image would require more headroom than the entire cache (`imageBytes × cache.size-multiplier > maxBytes`), the server returns **HTTP 507 Insufficient Storage**. This prevents one oversized image from thrashing the entire cache. Raise `cache.max-images` or `cache.estimated-image-bytes` (which feeds the auto-sizing formula) to resolve it.

**First-write-wins for derived tiles.** If two ImageServer pods concurrently process the same tile and both call `putDerived`, the second write is silently discarded. Processing is deterministic, so both produce identical bytes; the discard avoids unnecessary write operations.

**On restart.** The cache is in-memory only and empties on restart. Every cache miss falls back to the storage backend transparently — no warm-up step required.

### Cache sizing

The maximum cache size is calculated automatically at CacheServer startup:

```
maxBytes = min(
    containerMemoryLimit − jvmMaxHeap − cache.memory-overhead-bytes,
    cache.max-images × cache.estimated-image-bytes
)
```

Container memory is read from cgroup v2 (`/sys/fs/cgroup/memory.max`) with a fallback to cgroup v1. If the result is smaller than `cache.estimated-image-bytes` the server refuses to start with a clear error message rather than running in a configuration that guarantees 507 errors.

**Sizing rule of thumb:**

| Scenario | Recommended cache memory |
|---|---|
| 1 image, no tile reuse | `1 × largest source image` (absolute minimum) |
| 1 image, full tile pyramid | `5 × largest source image` |
| ~15 concurrent images | `20 × largest source image` (recommended default) |

For 500 MB source images the recommended starting point is **10 GB** for the CacheServer pod (`20 × 500 MB`). The 20× factor accounts for original bytes plus all zoom-level tile variants across multiple concurrent viewers.

### gRPC transport and mTLS

ImageServer communicates with CacheServer over **gRPC** (Protocol Buffers over HTTP/2):

- **Binary framing** — lower overhead than JSON/REST for large byte payloads.
- **HTTP/2 multiplexing** — many concurrent tile requests share one TCP connection; no head-of-line blocking per request.
- **Streaming** — source images stream as 1 MB chunks (configurable) rather than being buffered in a single message. A 500 MB original arrives in ~500 chunks.
- **Typed contract** — `shared/src/main/proto/cache.proto` is the single source of truth for both sides; schema drift is a compile error.

The gRPC service definition:

```protobuf
service CacheService {
    rpc GetOriginal (IdentifierRequest)     returns (stream Chunk);
    rpc GetInfo     (IdentifierRequest)     returns (CachedImageInfo);
    rpc PutInfo     (PutInfoRequest)        returns (google.protobuf.Empty);
    rpc GetDerived  (DerivedRequest)        returns (stream Chunk);
    rpc PutDerived  (stream DerivedChunk)   returns (PutResponse);
    rpc Evict       (IdentifierRequest)     returns (google.protobuf.Empty);
    rpc Stats       (google.protobuf.Empty) returns (CacheStats);
}
```

`GetOriginal` and `GetDerived` return `NOT_FOUND` (gRPC status) when the identifier or tile is not in the cache (or storage backend). The ImageServer interprets this as a cache miss and computes the result.

**mTLS.** Every ImageServer → CacheServer call is protected by mutual TLS. Each side presents a certificate signed by the internal CA; the CacheServer enforces `client-auth=REQUIRED`. In Kubernetes, cert-manager issues and auto-rotates certificates (1-year validity, renewed 30 days before expiry). In Docker Compose, `docker/gen-certs.sh` generates a self-signed CA and per-service certificates on first start.

**Fault tolerance** (`RemoteImageCache`):

| Method | On transient error | On NOT_FOUND |
|---|---|---|
| `getOriginal` | Retry ×3, then circuit breaker (5 s cooldown) | Throw `ImageNotFoundException` → HTTP 404 |
| `getInfo` | Return `Optional.empty()` | Return `Optional.empty()` (triggers recompute) |
| `getDerived` | Return `Optional.empty()` | Return `Optional.empty()` (triggers recompute) |
| `putInfo` / `putDerived` | Log warning, continue | — |
| `evict` | Propagate | — |

---

## Storage Backends

The CacheServer loads source images through a pluggable `StorageBackend`. Select with `storage.type`.

### Filesystem (`storage.type=filesystem`)

Reads files from a local directory tree. The identifier is appended to the base directory as a relative path. Path traversal is blocked — any resolved path that escapes the base directory throws a `SecurityException`.

```properties
storage.type=filesystem
storage.directory=/data/images
```

| Identifier | Resolved path |
|---|---|
| `painting.tif` | `/data/images/painting.tif` |
| `archive/painting.tif` | `/data/images/archive/painting.tif` |

### S3 (`storage.type=s3`)

Reads objects from any S3-compatible store: AWS S3, MinIO, Ceph, Garage, etc.

**Bucket routing** — the first path segment of the identifier becomes the bucket name:

| Identifier | Bucket | Key |
|---|---|---|
| `archive/painting.tif` | `archive` | `painting.tif` |
| `painting.tif` | `{storage.s3.default-bucket}` | `painting.tif` |

```properties
storage.type=s3
storage.s3.region=us-east-1
storage.s3.default-bucket=images
# For non-AWS S3-compatible stores (MinIO, Ceph, etc.):
storage.s3.endpoint=http://minio:9000
```

AWS credentials are resolved through the standard AWS default credentials chain (environment variables `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`, EC2 instance profile, EKS IAM role annotation, etc.).

---

## Configuration Reference

### ImageServer configuration

File: `imageserver/src/main/resources/application.properties`

| Property | Default | Description |
|---|---|---|
| `rest.base-url` | `http://localhost:8080/` | Public base URL; used to build the `@id` field in `info.json` |
| `image-server.max-scale` | `2` | Maximum upscale factor relative to source dimensions |
| `image-server.max-width` | `4000` | Maximum output pixel width |
| `image-server.max-height` | `4000` | Maximum output pixel height |
| `image-server.max-area` | `16000000` | Maximum output area in pixels (width × height) |
| `image-server.tile-width` | `256` | Default tile width when source image carries no tile metadata |
| `image-server.tile-height` | `256` | Default tile height |
| `image-server.source-formats` | `jpg,jpeg,…` | Accepted source file extensions |
| `image-server.processing.jpeg-quality` | `90` | JPEG output quality (1–100) |
| `image-server.processing.png-compression` | `6` | PNG compression level (0–9) |
| `image-server.processing.webp-quality` | `80` | WebP output quality (1–100) |
| `image-client.cache.enabled` | `true` | Emit HTTP `Cache-Control` headers |
| `image-client.cache.max-age` | `31536000` | `max-age` seconds for browser caches (1 year) |
| `image-client.cache.shared-max-age` | `31536000` | `s-maxage` seconds for CDN / proxy caches |
| `image-client.cache.is-public` | `true` | Allow shared (CDN) caching |
| `image-client.cache.no-transform` | `true` | Prevent proxies from re-encoding images |
| `quarkus.grpc.clients.cache-service.host` | `cacheserver` | CacheServer hostname |
| `quarkus.grpc.clients.cache-service.port` | `9000` | CacheServer gRPC port |
| `quarkus.grpc.clients.cache-service.ssl.certificate` | `/certs/tls.crt` | Client certificate path (mTLS) |
| `quarkus.grpc.clients.cache-service.ssl.key` | `/certs/tls.key` | Client private key path |
| `quarkus.grpc.clients.cache-service.ssl.trust-certificate` | `/certs/ca.crt` | CA certificate to verify CacheServer |
| `cache.chunk-size-bytes` | `1048576` | gRPC streaming chunk size — must match CacheServer |

### CacheServer configuration

File: `cacheserver/src/main/resources/application.properties`

| Property | Default | Description |
|---|---|---|
| `storage.type` | `filesystem` | Storage backend: `filesystem` or `s3` |
| `storage.directory` | `/data/images` | Root directory for the filesystem backend |
| `storage.s3.region` | `us-east-1` | AWS region for S3 backend |
| `storage.s3.default-bucket` | _(required for s3)_ | Bucket for identifiers without a path prefix |
| `storage.s3.endpoint` | _(unset)_ | Endpoint override for S3-compatible stores |
| `cache.ttl-hours` | `24` | Hours before a cache entry expires and is evicted by the TTL sweep |
| `cache.max-images` | `10` | Maximum number of images held in cache concurrently |
| `cache.estimated-image-bytes` | `524288000` | Expected maximum source image size in bytes (500 MB) — used in auto-sizing |
| `cache.memory-overhead-bytes` | `536870912` | Memory reserved for OS and JVM overhead (512 MB) — subtracted from available memory |
| `cache.size-multiplier` | `20` | Minimum required cache headroom as a multiple of source image size |
| `cache.chunk-size-bytes` | `1048576` | gRPC streaming chunk size — must match ImageServer |
| `quarkus.grpc.server.port` | `9000` | gRPC server listening port |
| `quarkus.grpc.server.ssl.certificate` | `/certs/tls.crt` | Server TLS certificate path |
| `quarkus.grpc.server.ssl.key` | `/certs/tls.key` | Server private key path |
| `quarkus.grpc.server.ssl.trust-certificate` | `/certs/ca.crt` | CA certificate to verify ImageServer client certs |
| `quarkus.grpc.server.ssl.client-auth` | `REQUIRED` | mTLS enforcement (`REQUIRED` or `NONE`) |
| `quarkus.management.port` | `9001` | Management port for metrics and admin endpoints |

---

## Local Setup from Source

### Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java | 25 | [sdkman.io](https://sdkman.io): `sdk install java 25-open` |
| Maven | 3.9+ | [maven.apache.org](https://maven.apache.org) or `sdk install maven` |
| libvips | 8.15+ | See below |

**Install libvips:**

```bash
# macOS (Homebrew)
brew install vips

# Ubuntu / Debian
sudo apt-get install libvips42 libvips-dev
```

Verify the installations:

```bash
vips --version      # e.g. vips-8.16.0-Tue Apr 15 ...
java -version       # openjdk version "25" ...
mvn -version        # Apache Maven 3.9.x ...
```

### Build all modules

```bash
git clone https://github.com/brigantinus/vips-is.git
cd vips-is

# Compile and package — shared → cacheserver → imageserver
mvn package -DskipTests
```

Successful output ends with:

```
[INFO] shared ..................................... SUCCESS
[INFO] cacheserver ................................ SUCCESS
[INFO] imageserver ................................ SUCCESS
[INFO] BUILD SUCCESS
```

The runnable JARs are at:

```
cacheserver/target/quarkus-app/quarkus-run.jar
imageserver/target/quarkus-app/quarkus-run.jar
```

### Point at your images

Edit `imageserver/src/main/resources/application.properties` (or pass as a system property at runtime):

```properties
rest.base-url=http://localhost:8080/
```

Edit `cacheserver/src/main/resources/application.properties`:

```properties
storage.type=filesystem
storage.directory=/absolute/path/to/your/image/directory
```

Or pass either value at the command line with `-Dstorage.directory=...`.

### Run

Open two terminals.

**Terminal 1 — start CacheServer:**

```bash
java \
  -Dquarkus.profile=dev \
  -jar cacheserver/target/quarkus-app/quarkus-run.jar
```

The `dev` profile disables mTLS (`client-auth=NONE`, no certificate paths required). CacheServer listens on:
- `localhost:9000` — gRPC (plain-text in dev mode)
- `localhost:9001` — management / metrics

**Terminal 2 — start ImageServer:**

```bash
# macOS (Homebrew — ARM/Apple Silicon)
java \
  --enable-native-access=ALL-UNNAMED \
  -Dquarkus.profile=dev \
  -Dvipsffm.libpath.vips.override=/opt/homebrew/lib/libvips.dylib \
  -Dvipsffm.libpath.glib.override=/opt/homebrew/lib/libglib-2.0.dylib \
  -Dvipsffm.libpath.gobject.override=/opt/homebrew/lib/libgobject-2.0.dylib \
  -jar imageserver/target/quarkus-app/quarkus-run.jar

# Linux (standard system paths)
java \
  --enable-native-access=ALL-UNNAMED \
  -Dquarkus.profile=dev \
  -jar imageserver/target/quarkus-app/quarkus-run.jar
```

The `dev` profile configures the gRPC client to connect to `localhost:9000` in plain-text mode. ImageServer listens on:
- `localhost:8080` — HTTP (IIIF API)
- `localhost:9000` — management / metrics

### Verify the setup

```bash
# Existence check (HEAD request)
curl -I http://localhost:8080/iiif/v3/image/myimage.tif/info.json

# Full image metadata
curl http://localhost:8080/iiif/v3/image/myimage.tif/info.json | jq .

# Fetch a 256×256 tile
curl -o tile.jpg \
  "http://localhost:8080/iiif/v3/image/myimage.tif/0,0,256,256/256,/0/default.jpg"

# Cache statistics
curl http://localhost:9001/metrics/json | jq .

# Force-evict an image from the cache
curl -X DELETE http://localhost:9001/cache/myimage.tif
```

### Run tests

```bash
# Unit tests (no libvips or running services needed)
mvn test -pl shared,imageserver -DskipITs
```

---

## Docker Compose

Docker Compose runs the full stack locally: an nginx ingress, three ImageServer replicas, one CacheServer, and an init service that generates mTLS certificates automatically on first start.

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) 24+ with Compose V2
- Maven 3.9+ and Java 25 (to build the JARs before building Docker images)

### Build

```bash
# 1. Build all Maven modules
mvn package -DskipTests

# 2. Build the Docker images (context is the project root)
docker build -f docker/vips-is-imageserver.Dockerfile \
  -t ghcr.io/brigantinus/vips-is/imageserver:latest .

docker build -f docker/vips-is-cacheserver.Dockerfile \
  -t ghcr.io/brigantinus/vips-is/cacheserver:latest .
```

### Configure image storage

By default the CacheServer reads source images from the `image-storage` Docker named volume, which starts empty. Point it at a local directory with a bind mount in `docker-compose.yml`:

```yaml
volumes:
  image-storage:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /absolute/path/to/your/images
```

### Start

```bash
docker compose up
```

On first start, `gen-certs` generates the internal CA and service certificates into the `certs` volume. All other services wait for completion before starting.

Expected startup sequence:

```
gen-certs     | Generating internal CA...
gen-certs     | Generating cacheserver certificate...
gen-certs     | Generating imageserver certificate...
gen-certs     | Done.
cacheserver-1 | Cache initialised: maxBytes=3.7 GB, ttl=24h
imageserver-1 | Started in 3.2s. Listening on: http://0.0.0.0:8080
imageserver-2 | Started in 3.4s. Listening on: http://0.0.0.0:8080
imageserver-3 | Started in 3.5s. Listening on: http://0.0.0.0:8080
nginx-1       | ready
```

### Test

```bash
# Image metadata
curl http://localhost/iiif/v3/image/myimage.tif/info.json

# Tile request (routed to one of the three ImageServer replicas via consistent hash)
curl -o tile.jpg "http://localhost/iiif/v3/image/myimage.tif/0,0,256,256/256,/0/default.jpg"

# Cache statistics (direct to CacheServer management port)
curl http://localhost:9001/metrics/json

# Force-evict an image from the cache
curl -X DELETE http://localhost:9001/cache/myimage.tif
```

### Disable mTLS (development / debugging)

```bash
QUARKUS_PROFILE=dev docker compose up
```

The `dev` Quarkus profile sets `client-auth=NONE` on the CacheServer and `plain-text=true` on the ImageServer gRPC client. No certificates are loaded. The `gen-certs` service still runs but its output is unused.

### Service map

| Service | Role | Exposed port |
|---|---|---|
| `gen-certs` | One-shot cert generator (Alpine + openssl), exits after first run | — |
| `cacheserver` | Off-heap cache + gRPC server | `9001` (management) |
| `imageserver-1` / `2` / `3` | IIIF REST + libvips processing | — (via nginx) |
| `nginx` | Ingress: consistent-hash load balancer | **`80`** |

nginx uses a `map` directive to extract the IIIF identifier from the request URI into `$iiif_id`, then `hash $iiif_id consistent` routes all requests for the same image to the same ImageServer replica. All tile requests for a given image land on the same pod, keeping that pod's derived-tile cache warm.

---

## Kubernetes

The provided `kubernetes.yml` deploys the full stack into a `vips-is` namespace with cert-manager-managed mTLS, a PodDisruptionBudget for the CacheServer, and a HorizontalPodAutoscaler for ImageServer replicas.

### Prerequisites

Install the required cluster dependencies:

```bash
# NGINX Ingress Controller
helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx --create-namespace

# cert-manager (with CRDs)
helm upgrade --install cert-manager cert-manager \
  --repo https://charts.jetstack.io \
  --namespace cert-manager --create-namespace \
  --set crds.enabled=true
```

### Push images

```bash
# Build and push to a registry accessible from your cluster
docker build -f docker/vips-is-imageserver.Dockerfile \
  -t ghcr.io/brigantinus/vips-is/imageserver:latest . && \
  docker push ghcr.io/brigantinus/vips-is/imageserver:latest

docker build -f docker/vips-is-cacheserver.Dockerfile \
  -t ghcr.io/brigantinus/vips-is/cacheserver:latest . && \
  docker push ghcr.io/brigantinus/vips-is/cacheserver:latest
```

Replace `ghcr.io/brigantinus/vips-is` with your own registry in `kubernetes.yml`, `docker/vips-is-imageserver.Dockerfile`, and `docker/vips-is-cacheserver.Dockerfile`.

### Deploy

```bash
kubectl apply -f kubernetes.yml

# Watch the rollout
kubectl rollout status deployment/cacheserver -n vips-is
kubectl rollout status deployment/imageserver  -n vips-is
```

### Configure the ingress hostname

Edit `kubernetes.yml` and replace `images.example.com` with your actual domain in both the `tls` section and the `rules` section of the Ingress resource:

```yaml
spec:
  tls:
    - hosts:
        - images.your-domain.com     # ← your domain
      secretName: ingress-tls
  rules:
    - host: images.your-domain.com   # ← same domain
```

The `ingress-tls` Secret for the public-facing certificate is separate from the internal mTLS secrets managed by the internal CA. Provision it manually or add a cert-manager `Certificate` resource pointing at an ACME `ClusterIssuer` (e.g. Let's Encrypt).

### Resource summary

| Resource | Kind | Notes |
|---|---|---|
| `vips-is` | Namespace | All resources isolated here |
| `vips-is-selfsigned` | ClusterIssuer | Bootstraps the internal CA |
| `vips-is-ca` | Certificate | 10-year internal CA |
| `vips-is-ca-issuer` | Issuer | Signs service leaf certificates |
| `cacheserver-tls` | Certificate | 1-year, auto-renewed; server + client auth |
| `imageserver-tls` | Certificate | 1-year, auto-renewed; client auth only |
| `image-storage` | PersistentVolumeClaim | 200 Gi for filesystem storage backend |
| `cacheserver` | Deployment | `replicas: 1` — never scale above 1 |
| `cacheserver` | Service | ClusterIP, ports 9000 (gRPC) and 9001 (mgmt) |
| `cacheserver-pdb` | PodDisruptionBudget | `minAvailable: 1` — protects in-memory cache during node drain |
| `imageserver` | Deployment | `replicas: 3` default |
| `imageserver` | Service | ClusterIP, ports 8080 (HTTP) and 9000 (mgmt) |
| `imageserver` | Ingress | NGINX with consistent-hash routing by IIIF identifier |
| `imageserver-hpa` | HorizontalPodAutoscaler | 2–10 replicas, target 70% CPU |

### Use S3 as storage backend

Create a Secret with AWS credentials:

```bash
kubectl create secret generic aws-credentials \
  --namespace vips-is \
  --from-literal=access-key-id=YOUR_KEY \
  --from-literal=secret-access-key=YOUR_SECRET
```

In `kubernetes.yml`, update the CacheServer deployment:

```yaml
env:
  - name: STORAGE_TYPE
    value: "s3"
  - name: STORAGE_S3_DEFAULT__BUCKET
    value: "your-bucket-name"
  - name: STORAGE_S3_REGION
    value: "eu-central-1"
  - name: AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: aws-credentials
        key: access-key-id
  - name: AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: aws-credentials
        key: secret-access-key
```

Remove the `image-storage` PVC and its volumeMount from the CacheServer deployment. For EKS, use an IAM role annotation on the pod instead of a Secret.

### Scaling

```bash
# Manually scale ImageServer
kubectl scale deployment imageserver --replicas=6 -n vips-is

# The HPA manages scaling automatically between 2 and 10 replicas.

# CacheServer must always stay at exactly 1 replica — it holds all cached state.
# A rolling restart causes a shared cold-cache event; all images self-heal on next access.
```

### Useful operational commands

```bash
# Stream logs
kubectl logs -l app=cacheserver -n vips-is --tail=100 -f
kubectl logs -l app=imageserver -n vips-is --tail=100 -f

# Cache statistics
kubectl port-forward svc/cacheserver 9001:9001 -n vips-is &
curl http://localhost:9001/metrics/json | jq '{entries: .entries, hitRate: .hitRate, usedMB: (.currentBytes/1048576|floor)}'

# Prometheus metrics
curl http://localhost:9001/metrics/prometheus

# Force-evict a single image from the cache (URL-encode slashes in identifier)
curl -X DELETE http://localhost:9001/cache/archive%2Fpainting.tif

# Check mTLS certificate status
kubectl get certificates -n vips-is
kubectl describe certificate cacheserver-tls -n vips-is
```
