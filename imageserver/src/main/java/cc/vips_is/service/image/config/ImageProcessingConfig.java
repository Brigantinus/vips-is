package cc.vips_is.service.image.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "processing")
public interface ImageProcessingConfig {
    Integer jpegQuality();
    Integer pngCompression();
    Integer webpQuality();
}
