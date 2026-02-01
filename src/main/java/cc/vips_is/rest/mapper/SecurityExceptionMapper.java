package cc.vips_is.rest.mapper;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static cc.vips_is.rest.mapper.ResponseBuilder.buildResponse;

@Provider
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {
    @Context
    UriInfo uriInfo;
    
    @Override
    public Response toResponse(SecurityException e) {
        return buildResponse(Response.Status.FORBIDDEN, "Access Denied", e, uriInfo);
    }

}