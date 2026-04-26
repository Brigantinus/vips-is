package cc.vips_is.service.storage.filesystem;

import cc.vips_is.service.storage.StorageBackend;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import cc.vips_is.service.storage.exceptions.StorageException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
@Slf4j
public class FilesystemStorageBackend implements StorageBackend {

    @ConfigProperty(name = "storage.directory")
    String storageDirectory;

    private Path storagePath;

    @PostConstruct
    void init() {
        if (StringUtils.isBlank(storageDirectory)) {
            throw new IllegalStateException("storage.directory is not configured");
        }
        storagePath = Paths.get(storageDirectory).toAbsolutePath().normalize();

        if (!Files.exists(storagePath)) {
            throw new StorageException("Base directory does not exist.").addContextValue("path", storagePath);
        }
        if (!Files.isDirectory(storagePath)) {
            throw new StorageException("Base path is not a directory.").addContextValue("path", storagePath);
        }
        if (!Files.isReadable(storagePath)) {
            throw new StorageException("Base directory is not readable.").addContextValue("path", storagePath);
        }
        log.info("Filesystem storage backend initialised: {}", storagePath);
    }

    @Override
    public byte[] load(String identifier) {
        Path resolved = resolve(identifier);
        try {
            return Files.readAllBytes(resolved);
        } catch (IOException e) {
            throw new StorageException("Failed to read image", e).addContextValue("identifier", identifier);
        }
    }

    @Override
    public boolean exists(String identifier) {
        if (StringUtils.isBlank(identifier)) return false;
        try {
            return Files.exists(resolve(identifier));
        } catch (SecurityException e) {
            return false;
        }
    }

    private Path resolve(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            throw new ImageNotFoundException("Identifier cannot be blank");
        }
        Path resolved = storagePath.resolve(identifier).normalize();
        if (!resolved.startsWith(storagePath)) {
            log.warn("Path traversal attempt blocked: {}", identifier);
            throw new SecurityException("Access denied");
        }
        if (!Files.exists(resolved)) {
            throw new ImageNotFoundException("Image not found").addContextValue("identifier", identifier);
        }
        return resolved;
    }
}
