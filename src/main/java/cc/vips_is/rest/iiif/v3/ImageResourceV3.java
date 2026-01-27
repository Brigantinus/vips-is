package cc.vips_is.rest.iiif.v3;

import cc.vips_is.rest.config.CacheConfigured;
import cc.vips_is.service.image.ImageService;
import cc.vips_is.service.image.model.*;
import cc.vips_is.service.image.model.*;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/iiif/v3/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ImageResourceV3 {

    @ConfigProperty(name = "rest.base-url")
    String baseUrl;

    @Inject
    ImageService imageService;

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
                    SizeInfo.fromV3String(size),
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
                    .header("Link", getCanonicalLink(identifier))
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
        return String.format("<%s/iiif/v3/%s/full/max/0/default.jpg>; rel=\"canonical\"", base, encodedId);
    }

}