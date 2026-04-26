package cc.vips_is.service.storage;

import cc.vips_is.service.storage.filesystem.FilesystemStorageBackend;
import cc.vips_is.service.storage.s3.S3StorageBackend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
public class StorageBackendProducer {

    @ConfigProperty(name = "storage.type", defaultValue = "filesystem")
    String storageType;

    @Inject
    Instance<FilesystemStorageBackend> filesystemBackend;

    @Inject
    Instance<S3StorageBackend> s3Backend;

    @Produces
    @ApplicationScoped
    public StorageBackend produce() {
        return switch (storageType.toLowerCase()) {
            case "filesystem" -> {
                log.info("Using filesystem storage backend");
                yield filesystemBackend.get();
            }
            case "s3" -> {
                log.info("Using S3 storage backend");
                yield s3Backend.get();
            }
            default -> throw new IllegalArgumentException("Unknown storage.type: " + storageType + " (valid: filesystem, s3)");
        };
    }
}
