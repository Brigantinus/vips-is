package cc.vips_is.rest.mapper;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.storage.exceptions.StorageException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomExceptionMapper {

    @Provider
    public static class NotFoundMapper implements ExceptionMapper<ImageNotFoundException> {
        @Context UriInfo uriInfo;
        @Override
        public Response toResponse(ImageNotFoundException e) {
            return buildResponse(Response.Status.NOT_FOUND, "Resource Not Found", e, uriInfo);
        }
    }

    @Provider
    public static class InvalidParameterMapper implements ExceptionMapper<InvalidParameterException> {
        @Context UriInfo uriInfo;
        @Override
        public Response toResponse(InvalidParameterException e) {
            return buildResponse(Response.Status.BAD_REQUEST, "Invalid IIIF Parameter", e, uriInfo);
        }
    }

    @Provider
    public static class StorageMapper implements ExceptionMapper<StorageException> {
        @Context UriInfo uriInfo;
        @Override
        public Response toResponse(StorageException e) {
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Storage Error", e, uriInfo);
        }
    }

    @Provider
    public static class SecurityMapper implements ExceptionMapper<SecurityException> {
        @Context UriInfo uriInfo;
        @Override
        public Response toResponse(SecurityException e) {
            return buildResponse(Response.Status.FORBIDDEN, "Access Denied", e, uriInfo);
        }
    }

    @Provider
    public static class ProcessingMapper implements ExceptionMapper<ImageProcessingException> {
        @Context UriInfo uriInfo;
        @Override
        public Response toResponse(ImageProcessingException e) {
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Processing Error", e, uriInfo);
        }
    }

    private static Response buildResponse(Response.Status status, String title, Exception e, UriInfo uriInfo) {
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