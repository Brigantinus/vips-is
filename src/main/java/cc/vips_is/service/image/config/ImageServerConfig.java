package cc.vips_is.service.image.config;

import io.smallrye.config.ConfigMapping;

import java.util.Set;

@ConfigMapping(prefix = "image-server")
public interface ImageServerConfig {
    Double maxScale();
    Integer tileWidth();
    Integer tileHeight();
    Integer maxWidth();
    Integer maxHeight();
    Long maxArea();

    Set<String> sourceFormats();

    ImageProcessingConfig processing();

}
