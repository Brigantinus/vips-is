# VIPS Image Server

An IIIF-compliant image server built with Java and Quarkus, leveraging the VIPS image processing library via vips-ffm for Java binding to ensure efficient image manipulation.

## Status
This project is in a very early stage a proof of concept and is not intended for production use!

## Features

- **IIIF Image API v2 & v3 Support**: Fully compliant with the IIIF Image API specifications.
- **Image Processing**: Supports region selection, resizing, rotation, quality conversion, and format conversion.
- **High Performance**: Uses VIPS for native-level image processing performance.

## Prerequisites

- Java 25 or higher
- Maven 3.9+
- VIPS library (native dependencies)

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd VipsIS
```

2. Configure the project:

Create a `application.properties` file in the `src/main/resources` directory:

```properties
# Application Configuration
quarkus.application.name=vips-is
rest.base-url=http://localhost:8080

# Storage Configuration
storage.directory=/path/to/your/images

# Image Server Configuration
image-server.max-scale=1.0
image-server.tile-width=1024
image-server.tile-height=1024
image-server.max-width=4096
image-server.max-height=4096
image-server.max-area=16777216

# Supported Source Formats
image-server.source-formats=jpg,jpeg,png,tif,tiff,gif,jp2,webp

# Image Processing Configuration
processing.jpeg-quality=85
processing.png-compression=6
processing.webp-quality=85

# Image Client Cache Configuration
image-client.cache.enabled=true
image-client.cache.max-age=86400
image-client.cache.shared-max-age=86400
image-client.cache.is-public=true
image-client.cache.is-private=false
image-client.cache.no-cache=false
image-client.cache.no-store=false
image-client.cache.must-revalidate=false
image-client.cache.proxy-revalidate=false
image-client.cache.no-transform=false
```

3. Build the project:

```bash
mvn clean package
```

## Usage

### Running the Application

Start the application:

```bash
java -jar target/vips_is-0.0.1-SNAPSHOT-runner.jar
```

Or using Maven:

```bash
mvn quarkus:dev
```

### API Endpoints

#### Image Information

Get image information (info.json):
- **V2**: `GET /iiif/v2/image/{identifier}/info.json`
- **V3**: `GET /iiif/v3/image/{identifier}/info.json`

#### Image Processing

Process an image:
- **V2**: `GET /iiif/v2/image/{identifier}/{region}/{size}/{rotation}/{quality}.{format}`
- **V3**: `GET /iiif/v3/image/{identifier}/{region}/{size}/{rotation}/{quality}.{format}`

#### Health Check

Check if an image exists:
- **V3**: `HEAD /iiif/v3/image/{identifier}/info.json`

### Example Usage

#### Get Image Info

```bash
curl http://localhost:8080/iiif/v2/image/myimage.jpg/info.json
```

#### Process an Image

Resize an image to 500x500 pixels:
```bash
curl http://localhost:8080/iiif/v2/image/myimage.jpg/full/500,500/0/default.jpg
```

Crop and resize:
```bash
curl http://localhost:8080/iiif/v2/image/myimage.jpg/100,100,400,400/500,500/0/default.jpg
```

Rotate and convert format:
```bash
curl http://localhost:8080/iiif/v2/image/myimage.jpg/full/max/90/color.png
```

## Acknowledgments

- [IIIF](https://iiif.io/) - The International Image Interoperability Framework
- [VIPS](https://libvips.github.io/libvips/) - The image processing library
- 
- [Quarkus](https://quarkus.io/) - The Java framework
