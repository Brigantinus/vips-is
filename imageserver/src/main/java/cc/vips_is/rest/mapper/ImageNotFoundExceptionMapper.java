package cc.vips_is.rest.mapper;

import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static cc.vips_is.rest.mapper.ResponseBuilder.buildResponse;

@Provider
public class ImageNotFoundExceptionMapper implements ExceptionMapper<ImageNotFoundException> {
    @Context
    UriInfo uriInfo;
    
    @Override
    public Response toResponse(ImageNotFoundException e) {
        return buildResponse(Response.Status.NOT_FOUND, "Resource Not Found", e, uriInfo);
    }

}