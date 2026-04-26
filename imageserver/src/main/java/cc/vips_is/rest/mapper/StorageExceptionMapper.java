package cc.vips_is.rest.mapper;

import cc.vips_is.service.storage.exceptions.StorageException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static cc.vips_is.rest.mapper.ResponseBuilder.buildResponse;

@Provider
public class StorageExceptionMapper implements ExceptionMapper<StorageException> {
    @Context
    UriInfo uriInfo;
    
    @Override
    public Response toResponse(StorageException e) {
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Storage Error", e, uriInfo);
    }

}