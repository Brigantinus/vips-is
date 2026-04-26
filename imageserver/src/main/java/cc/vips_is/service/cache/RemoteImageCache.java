package cc.vips_is.service.cache;

import cc.vips_is.grpc.*;
import cc.vips_is.service.image.model.ImageInfo;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import cc.vips_is.service.storage.exceptions.StorageException;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class RemoteImageCache implements ImageCache {

    @GrpcClient("cache-service")
    MutinyCacheServiceGrpc.MutinyCacheServiceStub cacheClient;

    @ConfigProperty(name = "cache.chunk-size-bytes", defaultValue = "1048576")
    int chunkSize;

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 5, failureRatio = 0.5,
            delay = 5, delayUnit = ChronoUnit.SECONDS,
            failOn = StorageException.class, skipOn = ImageNotFoundException.class)
    @Retry(maxRetries = 3, retryOn = StorageException.class, abortOn = ImageNotFoundException.class)
    public byte[] getOriginal(String identifier) {
        try {
            List<Chunk> chunks = cacheClient
                    .getOriginal(identifierRequest(identifier))
                    .collect().asList()
                    .await().indefinitely();
            return reassemble(chunks);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new ImageNotFoundException("Image not found: " + identifier);
            }
            throw new StorageException("Cache unavailable for: " + identifier, e);
        }
    }

    @Override
    public Optional<ImageInfo> getInfo(String identifier) {
        try {
            CachedImageInfo proto = cacheClient
                    .getInfo(identifierRequest(identifier))
                    .await().indefinitely();
            return Optional.of(ProtoMapper.toImageInfo(proto));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.NOT_FOUND) {
                log.debug("Cache getInfo failed for {}: {}", identifier, e.getStatus());
            }
            return Optional.empty();
        }
    }

    @Override
    public void putInfo(String identifier, ImageInfo info) {
        try {
            PutInfoRequest req = PutInfoRequest.newBuilder()
                    .setIdentifier(identifier)
                    .setInfo(ProtoMapper.toCachedImageInfo(info))
                    .build();
            cacheClient.putInfo(req).await().indefinitely();
        } catch (Exception e) {
            log.warn("Cache putInfo failed for {}: {}", identifier, e.getMessage());
        }
    }

    @Override
    public Optional<byte[]> getDerived(String identifier, DerivedKey key) {
        try {
            List<Chunk> chunks = cacheClient
                    .getDerived(ProtoMapper.toDerivedRequest(identifier, key))
                    .collect().asList()
                    .await().indefinitely();
            return Optional.of(reassemble(chunks));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.NOT_FOUND) {
                log.debug("Cache getDerived failed for {}: {}", identifier, e.getStatus());
            }
            return Optional.empty();
        }
    }

    @Override
    public void putDerived(String identifier, DerivedKey key, byte[] bytes) {
        try {
            DerivedRequest keyProto = ProtoMapper.toDerivedRequest(identifier, key);
            Multi<DerivedChunk> stream = buildDerivedStream(keyProto, bytes);
            cacheClient.putDerived(stream).await().indefinitely();
        } catch (Exception e) {
            log.warn("Cache putDerived failed for {}: {}", identifier, e.getMessage());
        }
    }

    @Override
    public void evict(String identifier) {
        cacheClient.evict(identifierRequest(identifier)).await().indefinitely();
    }

    @Override
    public CacheStats stats() {
        cc.vips_is.grpc.CacheStats proto = cacheClient
                .stats(Empty.getDefaultInstance())
                .await().indefinitely();
        return new CacheStats(
                proto.getEntryCount(), proto.getCurrentBytes(),
                proto.getMaxBytes(), proto.getHitRate());
    }

    private static IdentifierRequest identifierRequest(String identifier) {
        return IdentifierRequest.newBuilder().setIdentifier(identifier).build();
    }

    private static byte[] reassemble(List<Chunk> chunks) {
        int total = chunks.stream().mapToInt(c -> c.getData().size()).sum();
        byte[] result = new byte[total];
        int offset = 0;
        for (Chunk c : chunks) {
            byte[] data = c.getData().toByteArray();
            System.arraycopy(data, 0, result, offset, data.length);
            offset += data.length;
        }
        return result;
    }

    private Multi<DerivedChunk> buildDerivedStream(DerivedRequest keyProto, byte[] bytes) {
        List<DerivedChunk> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < bytes.length) {
            int end = Math.min(offset + chunkSize, bytes.length);
            chunks.add(DerivedChunk.newBuilder()
                    .setKey(keyProto)
                    .setData(ByteString.copyFrom(bytes, offset, end - offset))
                    .build());
            offset = end;
        }
        return Multi.createFrom().items(chunks.stream());
    }
}
