package cc.vips_is.rest.mapper;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static cc.vips_is.rest.mapper.ResponseBuilder.buildResponse;

@Provider
public class InvalidParameterExceptionMapper implements ExceptionMapper<InvalidParameterException> {
    @Context
    UriInfo uriInfo;
    
    @Override
    public Response toResponse(InvalidParameterException e) {
        return buildResponse(Response.Status.BAD_REQUEST, "Invalid IIIF Parameter", e, uriInfo);
    }

}