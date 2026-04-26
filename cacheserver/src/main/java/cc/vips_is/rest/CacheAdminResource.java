package cc.vips_is.rest;

import cc.vips_is.service.cache.CacheStats;
import cc.vips_is.service.cache.ImageCache;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/cache")
@Slf4j
public class CacheAdminResource {

    @Inject
    ImageCache imageCache;

    @DELETE
    @Path("/{identifier:.+}")
    public Response evict(@PathParam("identifier") String identifier) {
        imageCache.evict(identifier);
        log.info("Admin-triggered eviction: {}", identifier);
        return Response.noContent().build();
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public CacheStats stats() {
        return imageCache.stats();
    }
}
