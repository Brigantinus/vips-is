package cc.vips_is.service.image.core;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.Vips;
import cc.vips_is.service.image.dto.ImageData;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@ApplicationScoped
@Slf4j
public class ImageDataReader {

    public ImageData readImageData(String identifier, Path imagePath) {
        log.debug("readImageData: identifier={}, imagePath={}", identifier, imagePath);

        final Integer[] dimensions = new Integer[4];
        try {
            Vips.run(arena -> {
                VImage image = VImage.newFromFile(arena, imagePath.toString());

                dimensions[0] = image.getWidth();
                dimensions[1] = image.getHeight();
                try {
                    dimensions[2] = image.getInt("tile-width");
                    dimensions[3] = image.getInt("tile-height");
                } catch (Exception e) {
                    log.trace("No tile metadata for {}, using default size", identifier);
                }
            });
            return new  ImageData(identifier ,dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
        } catch (Exception e) {
            log.error("Failed to read image metadata for identifier: {}", identifier, e);
            throw new ImageProcessingException("Could not extract metadata from image", e);
        }
    }
}
