package cc.vips_is.rest.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "image-client.cache")
@StaticInitSafe
public interface ImageCacheConfig {
    boolean enabled();
    int maxAge();
    Optional<Integer> sharedMaxAge();
    boolean isPublic();
    boolean isPrivate();
    boolean noCache();
    boolean noStore();
    boolean mustRevalidate();
    boolean proxyRevalidate();
    boolean noTransform();
}
