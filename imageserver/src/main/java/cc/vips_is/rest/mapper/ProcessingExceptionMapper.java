package cc.vips_is.rest.mapper;

import cc.vips_is.service.image.exceptions.ImageProcessingException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static cc.vips_is.rest.mapper.ResponseBuilder.buildResponse;

@Provider
public class ProcessingExceptionMapper implements ExceptionMapper<ImageProcessingException> {
    @Context
    UriInfo uriInfo;
    
    @Override
    public Response toResponse(ImageProcessingException e) {
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Processing Error", e, uriInfo);
    }

}