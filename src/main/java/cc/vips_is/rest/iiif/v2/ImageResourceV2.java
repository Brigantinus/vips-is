package cc.vips_is.rest.iiif.v2;

import cc.vips_is.rest.config.CacheConfigured;
import cc.vips_is.rest.iiif.v2.model.ImageInfoV2;
import cc.vips_is.service.image.ImageService;
import cc.vips_is.service.image.model.*;
import cc.vips_is.service.image.model.*;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/iiif/v2/image")
@Slf4j
public class ImageResourceV2 {
    private static final String JSON_LD = "application/ld+json";

    private static final String IIIF_V2_PATH =  "/iiif/v2/image/";

    private static final String LINK_LEVEL2 = "<http://iiif.io/api/image/2/level2.json>;rel=\"profile\"";

    @ConfigProperty(name = "rest.base-url")
    String baseUrl;

    @Inject
    ImageService imageService;


    @GET
    @Path("/{identifier}")
    public Response redirectToInfo(@PathParam("identifier") String identifier) {
        log.debug("Redirecting base URL for identifier={}", identifier);

        URI infoUri = UriBuilder.fromUri(baseUrl)
                .path(IIIF_V2_PATH)
                .path(URLEncoder.encode(identifier, StandardCharsets.UTF_8))
                .path("info.json")
                .build();

        return Response.seeOther(infoUri).build();
    }

    @GET
    @Path("/{identifier}/info.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImageInfo(@PathParam("identifier") String identifier) {
        log.debug("getImageInfo: identifier={}", identifier);

        ImageInfo imageInfo = imageService.getImageInfo(identifier);

        ImageInfoV2 imageInfoV2 = ImageInfoV2.builder()
                .id(baseUrl + IIIF_V2_PATH + URLEncoder.encode(imageInfo.getId(), StandardCharsets.UTF_8))
                .width(imageInfo.getWidth())
                .height(imageInfo.getHeight())
                .tiles(imageInfo.getTiles())
                .sizes(imageInfo.getSizes())
                .build();

        return Response.ok(imageInfoV2, JSON_LD)
                .header(HttpHeaders.LINK, LINK_LEVEL2)
                .build();
    }

    @GET
    @Path("/{identifier}/{region}/{size}/{rotation}/{quality}.{format}")
    @Produces({MediaType.APPLICATION_JSON, "image/*"})
    @Blocking
    @CacheConfigured
    public Response getProcessedImage(
            @PathParam("identifier") String identifier,
            @PathParam("region") String region,
            @PathParam("size") String size,
            @PathParam("rotation") String rotation,
            @PathParam("quality") String quality,
            @PathParam("format") String format) {

        log.info("getProcessedImage: id={}, region={}, size={}, rot={}, qual={}, format={}",
                identifier, region, size, rotation, quality, format);

        try {
            ImageRequest imageRequest = new ImageRequest(
                    identifier,
                    RegionInfo.fromString(region),
                    SizeInfo.fromV2String(size),
                    QualityMode.fromString(quality),
                    RotationInfo.fromString(rotation),
                    ImageFormat.fromString(format));

            StreamingOutput output = imageService.processImage(imageRequest);

            String filename =  String.format("%s.%s",
                    FilenameUtils.getBaseName(identifier),
                    imageRequest.imageFormat().getExtension());

            return Response.ok(output)
                    .type(imageRequest.imageFormat().getMimeType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.LINK, getCanonicalLink(identifier))
                    .header(HttpHeaders.LINK, LINK_LEVEL2)
                    .build();

        } catch (ContextedRuntimeException e) {
            throw e.addContextValue("identifier", identifier)
                    .addContextValue("region", region)
                    .addContextValue("size", size)
                    .addContextValue("rotation", rotation)
                    .addContextValue("quality", quality)
                    .addContextValue("format", format);
        }
    }

    private String getCanonicalLink(String id) {
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return String.format("<%s/iiif/v2/%s/full/max/0/default.jpg>; rel=\"canonical\"", base, encodedId);
    }
}