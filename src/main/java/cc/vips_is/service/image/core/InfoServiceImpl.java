package cc.vips_is.service.image.core;

import cc.vips_is.service.image.InfoService;
import cc.vips_is.service.image.config.ImageServerConfig;
import cc.vips_is.service.image.dto.ImageData;
import cc.vips_is.service.image.model.ImageInfo;
import cc.vips_is.service.storage.StorageService;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class InfoServiceImpl implements InfoService {

    @Inject
    ImageServerConfig config;

    @Inject
    StorageService storageService;

    @Inject
    ImageDataReader  imageDataReader;

    @Inject
    ImageInfoCalculator imageInfoCalculator;

    @Override
    public ImageInfo getImageInfo(String identifier) {
        log.debug("getImageInfo: identifier={}", identifier);

        validateExtension(identifier);

        Path imagePath = storageService.resolve(identifier);
        ImageData imageData = imageDataReader.readImageData(identifier, imagePath);
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

    void validateExtension(String identifier) {
        String extension = FilenameUtils.getExtension(identifier);
        if (!config.sourceFormats().contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid image format: " + extension);
        }
    }
}