package cc.vips_is.rest.iiif.v3;

import cc.vips_is.rest.config.CacheConfigured;
import cc.vips_is.rest.iiif.v3.model.ImageInfoV3;
import cc.vips_is.service.image.InfoService;
import cc.vips_is.service.image.model.ImageInfo;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/iiif/v3/image")
public class InfoResourceV3 {

    private static final String IIIF_V3_PATH =  "/iiif/v3/image/";

    public static final String IIIF_V3_CONTEXT = "http://iiif.io/api/image/3/context.json";

    private static final String JSON_LD = "application/ld+json";

    @Inject
    InfoService service;

    @ConfigProperty(name = "rest.base-url")
    String baseUrl;

    @GET
    @Path("/{identifier}/info.json")
    @Produces({JSON_LD, MediaType.APPLICATION_JSON})
    @CacheConfigured
    public Response getInfoV3(
            @PathParam("identifier") String identifier,
            @Context Request request) {

        ImageInfo imageInfo = service.getImageInfo(identifier);

        ImageInfoV3 imageInfoV3 = ImageInfoV3.builder()
                .id(baseUrl + IIIF_V3_PATH + URLEncoder.encode(imageInfo.getId(), StandardCharsets.UTF_8))
                .context(IIIF_V3_CONTEXT)
                .width(imageInfo.getWidth())
                .height(imageInfo.getHeight())
                .maxWidth(imageInfo.getMaxWidth())
                .maxHeight(imageInfo.getMaxHeight())
                .maxArea((long) imageInfo.getMaxWidth() * imageInfo.getMaxHeight())
                .sizes(imageInfo.getSizes())
                .tiles(imageInfo.getTiles())
                .build();

        MediaType responseType = matchMediaType(request);

        return Response.ok(imageInfoV3)
                .type(responseType)
                .build();
    }

    @HEAD
    @Path("/{identifier}/info.json")
    @Produces({JSON_LD, MediaType.APPLICATION_JSON})
    public Response exists(
            @PathParam("identifier") String identifier,
            @Context Request request) {

        boolean exists = service.exists(identifier);
        MediaType responseType = matchMediaType(request);

        return (exists ? Response.ok() : Response.status(NOT_FOUND))
                .type(responseType)
                .build();
    }

    MediaType matchMediaType(Request request) {
        Variant variant = request.selectVariant(
                Variant.mediaTypes(MediaType.valueOf(JSON_LD), MediaType.APPLICATION_JSON_TYPE)
                        .build());

        if (Objects.isNull(variant)) {
            return MediaType.valueOf(JSON_LD + ";profile=\"" + IIIF_V3_CONTEXT + "\"");
        } else {
            MediaType responseType = variant.getMediaType();
            if (responseType.toString().contains(JSON_LD)) {
                Map<String, String> params = new HashMap<>(responseType.getParameters());
                params.put("profile", IIIF_V3_CONTEXT);
                responseType = new MediaType(responseType.getType(), responseType.getSubtype(), params);
            }
            return responseType;
        }
    }
}
