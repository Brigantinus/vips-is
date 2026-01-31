package cc.vips_is.service.image.core;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VTarget;
import app.photofox.vipsffm.Vips;
import cc.vips_is.service.image.ImageService;
import cc.vips_is.service.image.config.ImageServerConfig;
import cc.vips_is.service.image.dto.ImageData;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.image.model.*;
import cc.vips_is.service.storage.StorageService;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class ImageServiceImpl implements ImageService {


    @Inject
    ImageServerConfig config;

    @Inject
    StorageService storageService;

    @Inject
    ImageInfoCalculator imageInfoCalculator;

    @Inject
    RegionCalculator regionCalculator;

    @Inject
    SizeCalculator sizeCalculator;

    @Inject
    VipsProcessor vipsProcessor;

    @Override
    public ImageInfo getImageInfo(String identifier) {
        log.debug("getImageInfo: identifier={}", identifier);

        validateExtension(identifier);

        Path imagePath = storageService.resolve(identifier);
        ImageData imageData = vipsProcessor.readImageData(identifier, imagePath);
        return imageInfoCalculator.calculateImageInfo(imageData);
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

                    Size size = sizeCalculator.calculateSize(request, imageWidth, imageHeight,
                            config.maxWidth(), config.maxHeight(), config.maxArea());

                    validateConstraints(size);

                    Region region = regionCalculator.calculateRegion(request.regionInfo(), imageWidth, imageHeight);

                    image = vipsProcessor.applyCrop(image, region, imageWidth, imageHeight);
                    image = vipsProcessor.applyResize(image, size);
                    image = vipsProcessor.applyRotation(image, request.rotationInfo());
                    image = vipsProcessor.applyQuality(image, request.qualityMode());

                    if (ImageFormat.TIF == request.imageFormat() || ImageFormat.JP2 == request.imageFormat()) {
                        vipsProcessor.writeToBuffer(image, output, request.imageFormat());
                    } else {
                        VTarget vTarget = VTarget.newFromOutputStream(arena, output);
                        vipsProcessor.writeToTarget(image, vTarget, request.imageFormat());
                    }
                });
                log.trace("Successfully processed {}", request.identifier());
            } catch (Exception e) {
                log.error("Vips execution failed for image: {}", request.identifier(), e);
                throw new ImageProcessingException("Error during image transformation", e);
            }
        };
    }

    void validateExtension(String identifier) {
        String extension = FilenameUtils.getExtension(identifier);
        if (!config.sourceFormats().contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid image format: " + extension);
        }
    }

    void validateConstraints(Size size) {
        if ((long) size.width() * size.height() > config.maxArea()) {
            throw new InvalidParameterException("The requested image area exceeds server limits.");
        }
        if (size.width() > config.maxWidth() || size.height() > config.maxHeight()) {
            throw new InvalidParameterException("The requested image sizes exceed server limits.");
        }
    }

}