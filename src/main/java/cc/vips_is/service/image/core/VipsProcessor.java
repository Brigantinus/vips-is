package cc.vips_is.service.image.core;

import app.photofox.vipsffm.*;
import app.photofox.vipsffm.enums.VipsDirection;
import app.photofox.vipsffm.enums.VipsInterpretation;
import app.photofox.vipsffm.enums.VipsOperationRelational;
import cc.vips_is.service.image.dto.ImageData;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.image.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
@Slf4j
public class VipsProcessor {

    private static final List<Double> TH_BITONAL = List.of(128.0);

    @Inject
    VipsSaveOptionsRegistry saveOptionsRegistry;

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

    public VImage applyCrop(VImage image, Region region, int imageWidth, int imageHeight) {
        log.debug("applyCrop: {}", region);
        if (region.left() == 0 && region.top() == 0
                && region.width() == imageWidth && region.height() == imageHeight) {
            return image;
        }
        return image.extractArea(region.left(), region.top(), region.width(), region.height());
    }

    public VImage applyResize(VImage image, Size target) {
        log.debug("applyResize: {}", target);
        double scaleX = (double) target.width() / image.getWidth();
        double scaleY = (double) target.height() / image.getHeight();

        if (Math.abs(scaleX - 1.0) < 0.0001 && Math.abs(scaleY - 1.0) < 0.0001) {
            return image;
        }

        return image.resize(scaleX, VipsOption.Double("vscale", scaleY));
    }

    public VImage applyRotation(VImage image, RotationInfo rotationInfo) {
        log.debug("applyRotation: {}", rotationInfo);
        if (rotationInfo.rotation() == 0.0f) {
            return image;
        }

        if (rotationInfo.mirrored()) {
            image = image.flip(VipsDirection.DIRECTION_HORIZONTAL);
        }
        return image.rotate(rotationInfo.rotation(), VipsOption.Boolean("linear", true));
    }

    public VImage applyQuality(VImage image, QualityMode quality) {
        log.debug("applyQuality: {}", quality);
        return switch (quality) {
            case DEFAULT, COLOR -> image;
            case GRAY -> image.colourspace(VipsInterpretation.INTERPRETATION_B_W);
            case BITONAL -> image.colourspace(VipsInterpretation.INTERPRETATION_B_W)
                    .relationalConst(VipsOperationRelational.OPERATION_RELATIONAL_MORE, TH_BITONAL)
                    .colourspace(VipsInterpretation.INTERPRETATION_B_W);
        };
    }

    public void writeToTarget(VImage image, VTarget vTarget, ImageFormat format) {
        log.debug("writeToTarget: {}", format);
        VipsOption[] options = saveOptionsRegistry.getOptions(format);

        switch (format) {
            case JPG -> image.jpegsaveTarget(vTarget, options);
            case PNG -> image.pngsaveTarget(vTarget, options);
            case GIF ->  image.gifsaveTarget(vTarget, options);
            case WEBP -> image.webpsaveTarget(vTarget, options);
            default -> throw new IllegalArgumentException("Format not supported for target: " + format);
        }
    }

    public void writeToBuffer(VImage image, OutputStream outputStream, ImageFormat format) {
        log.debug("writeToBuffer: {}", format);

        VipsOption[] options = saveOptionsRegistry.getOptions(format);

        VBlob vBlob = switch (format) {
            case TIF -> image.tiffsaveBuffer(options);
            case JP2 -> image.jp2ksaveBuffer(options);
            default -> throw new IllegalArgumentException("Format not supported for buffer: " + format);
        };
        ByteBuffer byteBuffer = vBlob.asArenaScopedByteBuffer();
        try (WritableByteChannel channel = Channels.newChannel(outputStream)) {
            while (byteBuffer.hasRemaining()) {
                channel.write(byteBuffer);
            }
        } catch(IOException ioe) {
            throw new ImageProcessingException("Error writing image from buffer to outputStream", ioe);
        }
    }
}
