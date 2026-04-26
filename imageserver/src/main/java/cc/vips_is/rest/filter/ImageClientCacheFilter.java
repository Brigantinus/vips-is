package cc.vips_is.rest.filter;

import cc.vips_is.rest.config.CacheConfigured;
import cc.vips_is.rest.config.ImageCacheConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.ArrayList;
import java.util.List;

@Provider
@CacheConfigured
public class ImageClientCacheFilter implements ContainerResponseFilter {

    @Inject
    ImageCacheConfig config;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (!config.enabled()) {
            return;
        }

        List<String> directives = new ArrayList<>();

        if (config.noStore()) {
            directives.add("no-store");
        } else if (config.noCache()) {
            directives.add("no-cache");
        } else {
            if (config.isPublic()) directives.add("public");
            if (config.isPrivate()) directives.add("private");

            directives.add("max-age=" + config.maxAge());
            config.sharedMaxAge().ifPresent(s -> directives.add("s-maxage=" + s));

            if (config.mustRevalidate()) directives.add("must-revalidate");
            if (config.proxyRevalidate()) directives.add("proxy-revalidate");
            if (config.noTransform()) directives.add("no-transform");
        }

        response.getHeaders().putSingle("Cache-Control", String.join(", ", directives));
    }
}
