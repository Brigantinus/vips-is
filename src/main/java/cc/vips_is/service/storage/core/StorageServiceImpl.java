package cc.vips_is.service.storage.core;

import cc.vips_is.service.storage.StorageService;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import cc.vips_is.service.storage.exceptions.StorageException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
@Slf4j
public class StorageServiceImpl implements StorageService {

    @ConfigProperty(name = "storage.directory")
    String storageDirectory;

    private Path storagePath;

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(storageDirectory)) {
            throw new IllegalStateException("Image input directory is not configured");
        }

        storagePath = Paths.get(storageDirectory).toAbsolutePath().normalize();;

        if (!Files.exists(storagePath)) {
            throw new StorageException("Base directory does not exist.")
                    .addContextValue("storagePath", storagePath);
        }

        // Check if directory is readable
        if (!Files.isReadable(storagePath)) {
            throw new StorageException("Base directory is not readable.")
                    .addContextValue("storagePath", storagePath);
        }

        // Check if it's actually a directory
        if (!Files.isDirectory(storagePath)) {
            throw new StorageException("Base path is not a directory.")
                    .addContextValue("storagePath", storagePath);
        }

        log.info("Storage service initialized with base directory: {}", storagePath);
    }

    @Override
    public Path resolve(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            throw new ImageNotFoundException("Identifier cannot be null or empty");
        }

        Path resolvedPath = storagePath.resolve(identifier).normalize();

        if (!resolvedPath.startsWith(storagePath)) {
            log.warn("Possible path traversal attempt blocked: {}", identifier);
            throw new SecurityException("Access Denied");
        }

        if (!Files.exists(resolvedPath)) {
            log.info("Image not found: {}", resolvedPath);
            throw new ImageNotFoundException("Image not found")
                    .addContextValue("identifier", identifier);
        }

        return resolvedPath;
    }

}
