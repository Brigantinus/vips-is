package cc.vips_is.service.cache;

public class ImageTooLargeForCacheException extends RuntimeException {

    public ImageTooLargeForCacheException(String identifier, long imageBytes, long maxCacheBytes, int multiplier) {
        super(String.format(
                "Image '%s' (%s) requires a minimum cache of %s (%d× rule) but cache is configured to %s. " +
                "Increase cache.max-bytes or reduce the source image size.",
                identifier,
                humanReadable(imageBytes),
                humanReadable(imageBytes * multiplier),
                multiplier,
                humanReadable(maxCacheBytes)
        ));
    }

    private static String humanReadable(long bytes) {
        if (bytes >= 1L << 30) return String.format("%.1f GB", bytes / (double) (1L << 30));
        if (bytes >= 1L << 20) return String.format("%.1f MB", bytes / (double) (1L << 20));
        return bytes + " B";
    }
}
