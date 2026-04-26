package cc.vips_is.service.image.core;

import cc.vips_is.service.image.config.ImageServerConfig;
import cc.vips_is.service.image.dto.ImageData;
import cc.vips_is.service.image.model.ImageInfo;
import cc.vips_is.service.image.model.Size;
import cc.vips_is.service.image.model.Tile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class ImageInfoCalculator {

    @Inject
    ImageServerConfig config;

    public ImageInfo calculateImageInfo(ImageData imageData) {
        log.debug("calculateImageInfo: imageData={}", imageData);
        ImageInfo info = new ImageInfo();
        info.setId(imageData.identifier());
        info.setWidth(imageData.width());
        info.setHeight(imageData.height());

        // Resolve tile dimensions: Metadata > Config
        info.setTileWith(Optional.ofNullable(imageData.tileWidth()).orElse(config.tileWidth()));
        info.setTileHeight(Optional.ofNullable(imageData.tileHeight()).orElse(config.tileHeight()));

        calculateConstraints(info);
        calculatePyramid(info);

        return info;
    }

    void calculateConstraints(ImageInfo info) {
        log.debug("calculateConstraints: info={}", info);
        // Calculate potential dimensions based on max scale (e.g., 2.0 = 200% size)
        int scaledWidth = (int) Math.round(info.getWidth() * config.maxScale());
        int scaledHeight = (int) Math.round(info.getHeight() * config.maxScale());

        // Respect the lower of: Scale Limit vs. Absolute Pixel Limit
        int actualMaxWidth = Math.min(config.maxWidth(), scaledWidth);
        int actualMaxHeight = Math.min(config.maxHeight(), scaledHeight);

        // Preserve Aspect Ratio
        double originalRatio = (double) info.getWidth() / info.getHeight();
        double targetRatio = (double) actualMaxWidth / actualMaxHeight;

        if (targetRatio > originalRatio) {
            actualMaxWidth = (int) Math.round(actualMaxHeight * originalRatio);
        } else {
            actualMaxHeight = (int) Math.round(actualMaxWidth / originalRatio);
        }

        info.setMaxWidth(actualMaxWidth);
        info.setMaxHeight(actualMaxHeight);
    }

    void calculatePyramid(ImageInfo info) {
        log.debug("calculatePyramid: info={}", info);
        List<Integer> scaleFactors = new ArrayList<>();
        List<Size> sizes = new ArrayList<>();

        int factor = 1;
        while (true) {
            int currentW = (int) Math.ceil((double) info.getWidth() / factor);
            int currentH = (int) Math.ceil((double) info.getHeight() / factor);

            // Skip factors that result in images larger than our allowed max bounds
            if (currentW > info.getMaxWidth() || currentH > info.getMaxHeight()) {
                factor *= 2;
                continue;
            }

            scaleFactors.add(factor);
            sizes.add(new Size(currentW, currentH));

            // Stop if the current layer fits within a single tile
            if (currentW <= info.getTileWith() && currentH <= info.getTileHeight()) break;

            factor *= 2;
        }

        info.setTiles(List.of(new Tile(info.getTileWith(), info.getTileHeight(), scaleFactors)));
        info.setSizes(sizes.reversed());
    }

}
