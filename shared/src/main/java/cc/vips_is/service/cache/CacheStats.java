package cc.vips_is.service.cache;

public record CacheStats(
        long entryCount,
        long currentBytes,
        long maxBytes,
        double hitRate
) {}
