package cc.vips_is.rest.mapper;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.rest.iiif.common.model.ErrorResponse;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.storage.exceptions.StorageException;
import jakarta.ws.rs.core.Context;
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
            ErrorResponse error = new ErrorResponse(
                    "Resource Not Found",
                    e.getMessage(),
                    uriInfo.getPath()
            );
            log.debug(error.toString());
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }

    @Provider
    public static class InvalidParameterMapper implements ExceptionMapper<InvalidParameterException> {
        @Context UriInfo uriInfo;

        @Override
        public Response toResponse(InvalidParameterException e) {
            ErrorResponse error = new ErrorResponse(
                    "Invalid parameter",
                    e.getMessage(),
                    uriInfo.getPath()
            );
            log.debug(error.toString());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    @Provider
    public static class StorageMapper implements ExceptionMapper<StorageException> {
        @Context UriInfo uriInfo;

        @Override
        public Response toResponse(StorageException e) {
            ErrorResponse error = new ErrorResponse(
                    "Storage Error",
                    e.getMessage(),
                    uriInfo.getPath()
            );
            log.error(error.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    @Provider
    public static class SecurityMapper implements ExceptionMapper<SecurityException> {
        @Context UriInfo uriInfo;

        @Override
        public Response toResponse(SecurityException e) {
            ErrorResponse error = new ErrorResponse(
                    "Forbidden",
                    "Access to the requested resource is denied.",
                    uriInfo.getPath()
            );
            log.warn(error.toString());
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        }
    }

    @Provider
    public static class ProcessingMapper implements ExceptionMapper<ImageProcessingException> {
        @Context UriInfo uriInfo;

        @Override
        public Response toResponse(ImageProcessingException e) {
            ErrorResponse error = new ErrorResponse(
                    "Processing Error",
                    e.getMessage(),
                    uriInfo.getPath()
            );
            log.error(error.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
}
