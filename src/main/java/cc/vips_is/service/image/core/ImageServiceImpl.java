package cc.vips_is.service.image.core;

import app.photofox.vipsffm.*;
import app.photofox.vipsffm.enums.*;
import cc.vips_is.service.image.ImageService;
import cc.vips_is.service.image.config.ImageServerConfig;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.image.model.*;
import cc.vips_is.service.image.model.*;
import cc.vips_is.service.storage.StorageService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class ImageServiceImpl implements ImageService {

    private static final List<Double> TH_BITONAL = List.of(128.0);

    private final Map<ImageFormat, VipsOption[]> saveOptionsCache = new EnumMap<>(ImageFormat.class);

    @Inject
    ImageServerConfig config;

    @Inject
    StorageService storageService;

    @PostConstruct
    public void init() {
        log.debug("Initializing Vips save options cache from configuration");

        saveOptionsCache.put(ImageFormat.JPG, new VipsOption[] {
                VipsOption.Int("Q", config.processing().jpegQuality()),
                VipsOption.Boolean("optimize_coding", true)
        });

        saveOptionsCache.put(ImageFormat.PNG, new VipsOption[] {
                VipsOption.Int("compression", config.processing().pngCompression())
        });

        saveOptionsCache.put(ImageFormat.WEBP, new VipsOption[] {
                VipsOption.Int("Q", config.processing().webpQuality())
        });

        saveOptionsCache.put(ImageFormat.TIF, new VipsOption[] {
                VipsOption.Enum("compression", VipsForeignTiffCompression.FOREIGN_TIFF_COMPRESSION_LZW),
                VipsOption.Enum("predictor", VipsForeignTiffPredictor.FOREIGN_TIFF_PREDICTOR_HORIZONTAL),
                VipsOption.Boolean("bigtiff", true)
        });

        saveOptionsCache.put(ImageFormat.GIF, new VipsOption[0]);
        saveOptionsCache.put(ImageFormat.JP2, new VipsOption[] {
                VipsOption.Boolean("lossless", false)
        });
    }

    @Override
    public StreamingOutput processImage(@NonNull ImageRequest request) {
        log.debug("processImage: request={}", request);

        String extension = FilenameUtils.getExtension(request.identifier());
        if (!config.sourceFormats().contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid image format: " + extension);
        }

        Path inputPath = storageService.resolve(request.identifier());

        return output -> {
            try {
                Vips.run(arena -> {
                    VImage image = VImage.newFromFile(arena, inputPath.toString());
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    Size size = SizeCalculator.calculateSize(request, imageWidth, imageHeight,
                            config.maxWidth(), config.maxHeight(), config.maxArea());

                    validateConstraints(size);

                    Region region = RegionCalculator.calculateRegion(request.regionInfo(), imageWidth, imageHeight);

                    image = applyCrop(image, region, imageWidth, imageHeight);
                    image = applyResize(image, size);
                    image = applyRotation(image, request.rotationInfo());
                    image = applyQuality(image, request.qualityMode());

                    if (ImageFormat.TIF == request.imageFormat() || ImageFormat.JP2 == request.imageFormat()) {
                        writeToBuffer(image, output, request.imageFormat());
                    } else {
                        VTarget vTarget = VTarget.newFromOutputStream(arena, output);
                        writeToTarget(image, vTarget, request.imageFormat());
                    }
                });
                log.trace("Successfully processed {}", request.identifier());
            } catch (Exception e) {
                log.error("Vips execution failed for image: {}", request.identifier(), e);
                throw new ImageProcessingException("Error during image transformation", e);
            }
        };
    }

    private void validateConstraints(Size size) {
        if ((long) size.width() * size.height() > config.maxArea()) {
            throw new InvalidParameterException("The requested image area exceeds server limits.");
        }
        if (size.width() > config.maxWidth() || size.height() > config.maxHeight()) {
            throw new InvalidParameterException("The requested image sizes exceed server limits.");
        }
    }

    private VImage applyCrop(VImage image, Region region, int imageWidth, int imageHeight) {
        log.debug("applyCrop: {}", region);
        if (region.left() == 0 && region.top() == 0
                && region.width() == imageWidth && region.height() == imageHeight) {
            return image;
        }
        return image.extractArea(region.left(), region.top(), region.width(), region.height());
    }

    private VImage applyResize(VImage image, Size target) {
        log.debug("applyResize: {}", target);
        double scaleX = (double) target.width() / image.getWidth();
        double scaleY = (double) target.height() / image.getHeight();

        if (Math.abs(scaleX - 1.0) < 0.0001 && Math.abs(scaleY - 1.0) < 0.0001) {
            return image;
        }

        return image.resize(scaleX, VipsOption.Double("vscale", scaleY));
    }

    private VImage applyRotation(VImage image, RotationInfo rotationInfo) {
        log.debug("applyRotation: {}", rotationInfo);
        if (rotationInfo.rotation() == 0) {
            return image;
        }

        if (rotationInfo.mirrored()) {
            image = image.flip(VipsDirection.DIRECTION_HORIZONTAL);
        }
        return image.rotate(rotationInfo.rotation(), VipsOption.Boolean("linear", true));
    }

    private VImage applyQuality(VImage image, QualityMode quality) {
        log.debug("applyQuality: {}", quality);
        return switch (quality) {
            case DEFAULT, COLOR -> image;
            case GRAY -> image.colourspace(VipsInterpretation.INTERPRETATION_B_W);
            case BITONAL -> image.colourspace(VipsInterpretation.INTERPRETATION_B_W)
                    .relationalConst(VipsOperationRelational.OPERATION_RELATIONAL_MORE, TH_BITONAL)
                    .colourspace(VipsInterpretation.INTERPRETATION_B_W);
        };
    }

    private void writeToTarget(VImage image, VTarget vTarget, ImageFormat format) {
        log.debug("writeToTarget: {}", format);
        VipsOption[] options = saveOptionsCache.getOrDefault(format, new VipsOption[0]);

        switch (format) {
            case JPG -> image.jpegsaveTarget(vTarget, options);
            case PNG -> image.pngsaveTarget(vTarget, options);
            case GIF ->  image.gifsaveTarget(vTarget, options);
            case WEBP -> image.webpsaveTarget(vTarget, options);
            default -> throw new IllegalArgumentException("Format not supported for target: " + format);
        }
    }

    private void writeToBuffer(VImage image, OutputStream outputStream, ImageFormat format) {
        log.debug("writeToBuffer: {}", format);

        VipsOption[] options = saveOptionsCache.getOrDefault(format, new VipsOption[0]);

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