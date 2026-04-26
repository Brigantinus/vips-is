package cc.vips_is.rest.mapper;

import cc.vips_is.service.cache.ImageTooLargeForCacheException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class TooLargeForCacheExceptionMapper implements ExceptionMapper<ImageTooLargeForCacheException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ImageTooLargeForCacheException e) {
        String message = String.format("[507] Image Too Large for Cache: %s (Path: %s)",
                e.getMessage(), uriInfo.getPath());
        log.warn(message);
        return Response.status(507)
                .type(MediaType.TEXT_PLAIN)
                .entity(message)
                .build();
    }
}
