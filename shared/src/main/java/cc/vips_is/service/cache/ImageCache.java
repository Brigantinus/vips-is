package cc.vips_is.service.cache;

import cc.vips_is.service.image.model.ImageInfo;

import java.util.Optional;

public interface ImageCache {

    byte[] getOriginal(String identifier);

    Optional<ImageInfo> getInfo(String identifier);

    void putInfo(String identifier, ImageInfo info);

    Optional<byte[]> getDerived(String identifier, DerivedKey key);

    void putDerived(String identifier, DerivedKey key, byte[] bytes);

    void evict(String identifier);

    cc.vips_is.service.cache.CacheStats stats();
}
