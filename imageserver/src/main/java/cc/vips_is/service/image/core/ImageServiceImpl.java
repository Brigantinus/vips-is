package cc.vips_is.service.image.core;

import cc.vips_is.service.cache.DerivedKey;
import cc.vips_is.service.cache.ImageCache;
import cc.vips_is.service.image.ImageService;
import cc.vips_is.service.image.config.ImageServerConfig;
import cc.vips_is.service.image.dto.ImageData;
import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.image.model.*;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ApplicationScoped
@Slf4j
public class ImageServiceImpl implements ImageService {

    @Inject
    ImageServerConfig config;

    @Inject
    ImageCache imageCache;

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

        Optional<ImageInfo> cached = imageCache.getInfo(identifier);
        if (cached.isPresent()) {
            return cached.get();
        }

        byte[] bytes = imageCache.getOriginal(identifier);
        ImageData imageData = vipsProcessor.readImageData(identifier, bytes);
        ImageInfo info = imageInfoCalculator.calculateImageInfo(imageData);
        imageCache.putInfo(identifier, info);
        return info;
    }

    @Override
    public boolean exists(String identifier) {
        log.debug("exists: identifier={}", identifier);
        if (imageCache.getInfo(identifier).isPresent()) {
            return true;
        }
        try {
            imageCache.getOriginal(identifier);
            return true;
        } catch (ImageNotFoundException e) {
            return false;
        }
    }

    @Override
    public StreamingOutput processImage(@NonNull ImageRequest request) {
        log.debug("processImage: request={}", request);

        DerivedKey derivedKey = new DerivedKey(
                request.regionInfo(), request.sizeInfo(), request.rotationInfo(),
                request.qualityMode(), request.imageFormat());

        Optional<byte[]> cached = imageCache.getDerived(request.identifier(), derivedKey);
        if (cached.isPresent()) {
            log.trace("Derived cache hit: {}", request.identifier());
            byte[] hit = cached.get();
            return output -> output.write(hit);
        }

        byte[] originalBytes = imageCache.getOriginal(request.identifier());
        ImageData imageData = vipsProcessor.readImageData(request.identifier(), originalBytes);
        int width = imageData.width();
        int height = imageData.height();

        Size size = sizeCalculator.calculateSize(request, width, height, config.maxWidth(), config.maxHeight(), config.maxArea());
        validateConstraints(size);
        Region region = regionCalculator.calculateRegion(request.regionInfo(), width, height);

        byte[] derived;
        try {
            derived = vipsProcessor.processToBytes(originalBytes, region, size, request.rotationInfo(), request.qualityMode(), request.imageFormat());
        } catch (Exception e) {
            log.error("Vips execution failed for image: {}", request.identifier(), e);
            throw new ImageProcessingException("Error during image transformation", e);
        }

        imageCache.putDerived(request.identifier(), derivedKey, derived);

        return output -> output.write(derived);
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
