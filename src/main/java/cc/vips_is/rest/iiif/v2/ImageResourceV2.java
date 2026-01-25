package cc.vips_is.rest.iiif.v2;

import cc.vips_is.rest.config.CacheConfigured;
import cc.vips_is.rest.iiif.common.model.IIIFImageRequest;
import cc.vips_is.service.image.ImageService;
import cc.vips_is.service.image.model.*;
import cc.vips_is.service.image.model.*;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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

@Path("/iiif/v2/image")
@Slf4j
public class ImageResourceV2 {

    @ConfigProperty(name = "rest.base-url")
    String baseUrl;

    @Inject
    ImageService imageService;

    @GET
    @Path("/{identifier}/{region}/{size}/{rotation}/{quality}.{format}")
    @Produces({MediaType.APPLICATION_JSON, "image/*"})
    @Blocking
    @CacheConfigured
    public Response getProcessedImage(@BeanParam IIIFImageRequest request) {
        log.info("getProcessedImage: request={}", request);

        try {
            ImageRequest imageRequest = new ImageRequest(
                    request.getIdentifier(),
                    RegionInfo.fromString(request.getRegion()),
                    SizeInfo.fromV2String(request.getSize()),
                    QualityMode.fromString(request.getQuality()),
                    RotationInfo.fromString(request.getRotation()),
                    ImageFormat.fromString(request.getFormat()));

            StreamingOutput output = imageService.processImage(imageRequest);

            String filename =  String.format("%s.%s",
                    FilenameUtils.getBaseName(request.getIdentifier()),
                    imageRequest.imageFormat().getExtension());

            return Response.ok(output)
                    .type(imageRequest.imageFormat().getMimeType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header("Link", getCanonicalLink(request.getIdentifier()))
                    .build();

        } catch (ContextedRuntimeException e) {
            throw e.addContextValue("rawRequest", request);
        }
    }

    private String getCanonicalLink(String id) {
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return String.format("<%s/iiif/v2/%s/full/max/0/default.jpg>; rel=\"canonical\"", base, encodedId);
    }
}