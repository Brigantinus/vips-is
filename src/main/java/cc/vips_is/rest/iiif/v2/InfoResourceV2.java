package cc.vips_is.rest.iiif.v2;

import cc.vips_is.rest.iiif.v2.model.ImageInfoV2;
import cc.vips_is.service.image.ImageService;
import cc.vips_is.service.image.model.ImageInfo;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/iiif/v2/image")
@Slf4j
public class InfoResourceV2 {
    private static final String IIIF_V3_CONTEXT = "http://iiif.io/api/image/3/context.json";
    private static final String JSON_LD = "application/ld+json";

    private static final String IIIF_V2_PATH =  "/iiif/v2/image/";

    @Inject
    ImageService imageService;

    @ConfigProperty(name = "rest.base-url")
    String baseUrl;

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
                .build();
    }
}
