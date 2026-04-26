package cc.vips_is.service.storage.s3;

import cc.vips_is.service.storage.StorageBackend;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import cc.vips_is.service.storage.exceptions.StorageException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Map;
import java.util.Optional;

/**
 * S3-compatible storage backend.
 *
 * Bucket routing: the first path segment of the identifier is used as the bucket name.
 * e.g. "archive/image001.tif" → bucket "archive", key "image001.tif"
 *
 * A default bucket is used for identifiers without a path separator.
 * Override individual bucket names via storage.s3.bucket-map.<prefix>=<bucket>.
 *
 * Configure endpoint override for S3-compatible stores (MinIO, Ceph):
 *   storage.s3.endpoint=http://minio:9000
 */
@ApplicationScoped
@Typed(S3StorageBackend.class)
@Slf4j
public class S3StorageBackend implements StorageBackend {

    @ConfigProperty(name = "storage.s3.default-bucket")
    String defaultBucket;

    @Inject
    S3Client s3Client;

    @Override
    public byte[] load(String identifier) {
        BucketKey bk = resolve(identifier);
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bk.bucket()).key(bk.key()).build());
            return response.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ImageNotFoundException("Image not found").addContextValue("identifier", identifier);
        } catch (Exception e) {
            throw new StorageException("Failed to load image from S3", e).addContextValue("identifier", identifier);
        }
    }

    @Override
    public boolean exists(String identifier) {
        BucketKey bk = resolve(identifier);
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bk.bucket()).key(bk.key()).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("S3 head-object failed for {}", identifier, e);
            return false;
        }
    }

    private BucketKey resolve(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            throw new ImageNotFoundException("Identifier cannot be blank");
        }
        int slash = identifier.indexOf('/');
        if (slash > 0) {
            String prefix = identifier.substring(0, slash);
            String key    = identifier.substring(slash + 1);
            return new BucketKey(prefix, key);
        }
        return new BucketKey(defaultBucket, identifier);
    }

    private record BucketKey(String bucket, String key) {}
}
