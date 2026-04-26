package cc.vips_is.rest.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class ResponseBuilder {

    static Response buildResponse(Response.Status status, String title, Exception e, UriInfo uriInfo) {
        String message = String.format("[%s] %s: %s (Path: %s)",
                status.getStatusCode(), title, e.getMessage(), uriInfo.getPath());

        if (status.getFamily() == Response.Status.Family.SERVER_ERROR) {
            log.error(message, e);
        } else if (status == Response.Status.FORBIDDEN) {
            log.warn(message);
        } else {
            log.debug(message);
        }

        return Response.status(status)
                .type(MediaType.TEXT_PLAIN)
                .entity(message)
                .build();
    }
}
