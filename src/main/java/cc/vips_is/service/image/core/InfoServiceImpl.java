package cc.vips_is.service.image.core;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.Vips;
import cc.vips_is.service.image.InfoService;
import cc.vips_is.service.image.config.ImageServerConfig;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.image.model.ImageInfo;
import cc.vips_is.service.image.model.Size;
import cc.vips_is.service.image.model.Tile;
import cc.vips_is.service.storage.StorageService;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class InfoServiceImpl implements InfoService {

    @Inject
    ImageServerConfig config;

    @Inject
    StorageService storageService;

    @Override
    public ImageInfo getImageInfo(String identifier) {
        log.debug("getImageInfo: identifier={}", identifier);

        String extension = FilenameUtils.getExtension(identifier);
        if (!config.sourceFormats().contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid image format: " + extension);
        }

        ImageInfo info = new ImageInfo();
        info.setId(identifier);

        Integer[] imageDimensions = getImageDimensions(identifier);
        info.setWidth(imageDimensions[0]);
        info.setHeight(imageDimensions[1]);
        info.setTileWith(Optional.ofNullable(imageDimensions[2]).orElse(config.tileWidth()));
        info.setTileHeight(Optional.ofNullable(imageDimensions[3]).orElse(config.tileHeight()));

        calculateMaxWidthAndHeight(info);

        calculateTilesAndSizes(info);

        return info;
    }

    @Override
    public boolean exists(String identifier) {
        log.debug("exists: identifier={}", identifier);
        try {
            return Optional.ofNullable(storageService.resolve(identifier)).isPresent();
        } catch(ImageNotFoundException e) {
            return false;
        }
    }

    private Integer[] getImageDimensions(String identifier) {
        log.debug("getImageDimensions: identifier={}", identifier);

        Path inputPath = storageService.resolve(identifier);

        Integer[] dimensions = new Integer[4];
        try {
            Vips.run(arena -> {
                VImage image = VImage.newFromFile(arena, inputPath.toString());

                dimensions[0] = image.getWidth();
                dimensions[1] = image.getHeight();
                try {
                    dimensions[2] = image.getInt("tile-width");
                    dimensions[3] = image.getInt("tile-height");
                } catch (Exception e) {
                    log.trace("No tile metadata for {}, using default size", identifier);
                }
            });
            return dimensions;
        } catch (Exception e) {
            log.error("Failed to read image metadata for identifier: {}", identifier, e);
            throw new ImageProcessingException("Could not extract metadata from image", e);
        }
    }

    private void calculateMaxWidthAndHeight(ImageInfo info) {
        int originalWidth = info.getWidth();
        int originalHeight = info.getHeight();

        // Limits from ImageServerConfig
        int maxWidthLimit = config.maxWidth();
        int maxHeightLimit = config.maxHeight();

        // Dynamic scale limit from config (e.g., 1.0 for original size, 2.0 for 2x upscale)
        double maxScale = config.maxScale();

        // 1. Calculate max dimensions based on the scale constraint
        int scaledWidth = (int) Math.round(originalWidth * maxScale);
        int scaledHeight = (int) Math.round(originalHeight * maxScale);

        // 2. The effective maximum should respect BOTH pixel limits and scale limits
        int actualMaxWidth = Math.min(maxWidthLimit, scaledWidth);
        int actualMaxHeight = Math.min(maxHeightLimit, scaledHeight);

        // 3. Ensure aspect ratio is preserved within these bounds
        double originalRatio = (double) originalWidth / originalHeight;
        double targetRatio = (double) actualMaxWidth / actualMaxHeight;

        if (targetRatio > originalRatio) {
            // Box is too wide, constrain width based on height
            actualMaxWidth = (int) Math.round(actualMaxHeight * originalRatio);
        } else {
            // Box is too tall, constrain height based on width
            actualMaxHeight = (int) Math.round(actualMaxWidth / originalRatio);
        }

        info.setMaxWidth(actualMaxWidth);
        info.setMaxHeight(actualMaxHeight);
    }

    private void calculateTilesAndSizes(ImageInfo info) {
        List<Integer> scaleFactors = new ArrayList<>();
        List<Size> sizes = new ArrayList<>();

        int factor = 1;
        while (true) {
            int currentW = (int) Math.ceil((double) info.getWidth() / factor);
            int currentH = (int) Math.ceil((double) info.getHeight() / factor);

            if (currentW > info.getMaxWidth() || currentH > info.getMaxHeight()) {
                factor *= 2;
                continue;
            }

            scaleFactors.add(factor);
            sizes.add(new Size(currentW, currentH));

            if (currentW <= info.getTileWith() && currentH <= info.getTileHeight()) break;
            factor *= 2;
        }

        info.setTiles(List.of(new Tile(info.getTileWith(), info.getTileHeight(), scaleFactors)));
        info.setSizes(sizes);
    }
}
