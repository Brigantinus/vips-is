package cc.vips_is.grpc;

import cc.vips_is.service.cache.DerivedKey;
import cc.vips_is.service.cache.ImageCache;
import cc.vips_is.service.cache.ImageTooLargeForCacheException;
import cc.vips_is.service.image.model.ImageInfo;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;

@GrpcService
@Slf4j
public class CacheGrpcService extends MutinyCacheServiceGrpc.CacheServiceImplBase {

    @Inject
    ImageCache imageCache;

    @ConfigProperty(name = "cache.chunk-size-bytes", defaultValue = "1048576")
    int chunkSize;

    @Override
    public Multi<Chunk> getOriginal(IdentifierRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                byte[] bytes = imageCache.getOriginal(request.getIdentifier());
                emitChunks(emitter, bytes);
                emitter.complete();
            } catch (Exception e) {
                log.debug("GetOriginal failed for {}: {}", request.getIdentifier(), e.getMessage());
                emitter.fail(Status.NOT_FOUND
                        .withDescription("Original not found: " + request.getIdentifier())
                        .withCause(e)
                        .asRuntimeException());
            }
        });
    }

    @Override
    public Uni<CachedImageInfo> getInfo(IdentifierRequest request) {
        return Uni.createFrom().item(() -> {
            Optional<ImageInfo> info = imageCache.getInfo(request.getIdentifier());
            return info.map(ProtoMapper::toCachedImageInfo)
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("Info not cached: " + request.getIdentifier())
                            .asRuntimeException());
        });
    }

    @Override
    public Uni<Empty> putInfo(PutInfoRequest request) {
        return Uni.createFrom().item(() -> {
            ImageInfo info = ProtoMapper.toImageInfo(request.getInfo());
            imageCache.putInfo(request.getIdentifier(), info);
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public Multi<Chunk> getDerived(DerivedRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            DerivedKey key = ProtoMapper.toDerivedKey(request);
            Optional<byte[]> bytes = imageCache.getDerived(request.getIdentifier(), key);
            if (bytes.isEmpty()) {
                emitter.fail(Status.NOT_FOUND
                        .withDescription("Derived not cached: " + request.getIdentifier())
                        .asRuntimeException());
                return;
            }
            emitChunks(emitter, bytes.get());
            emitter.complete();
        });
    }

    @Override
    public Uni<PutResponse> putDerived(Multi<DerivedChunk> request) {
        return request.collect().asList().map(chunks -> {
            if (chunks.isEmpty()) {
                return PutResponse.newBuilder().setStored(false).build();
            }

            DerivedRequest keyProto = chunks.get(0).getKey();
            String identifier = keyProto.getIdentifier();
            DerivedKey key = ProtoMapper.toDerivedKey(keyProto);

            byte[] bytes = reassemble(chunks);

            try {
                imageCache.putDerived(identifier, key, bytes);
                return PutResponse.newBuilder().setStored(true).build();
            } catch (ImageTooLargeForCacheException e) {
                log.warn("Derived image too large for cache: {}", identifier);
                return PutResponse.newBuilder().setStored(false).build();
            }
        });
    }

    @Override
    public Uni<Empty> evict(IdentifierRequest request) {
        return Uni.createFrom().item(() -> {
            imageCache.evict(request.getIdentifier());
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public Uni<cc.vips_is.grpc.CacheStats> stats(Empty request) {
        return Uni.createFrom().item(() -> {
            cc.vips_is.service.cache.CacheStats s = imageCache.stats();
            return cc.vips_is.grpc.CacheStats.newBuilder()
                    .setEntryCount(s.entryCount())
                    .setCurrentBytes(s.currentBytes())
                    .setMaxBytes(s.maxBytes())
                    .setHitRate(s.hitRate())
                    .build();
        });
    }

    private void emitChunks(io.smallrye.mutiny.subscription.MultiEmitter<? super Chunk> emitter, byte[] bytes) {
        int offset = 0;
        while (offset < bytes.length) {
            int end = Math.min(offset + chunkSize, bytes.length);
            emitter.emit(Chunk.newBuilder()
                    .setData(ByteString.copyFrom(bytes, offset, end - offset))
                    .build());
            offset = end;
        }
    }

    private static byte[] reassemble(List<DerivedChunk> chunks) {
        int totalSize = chunks.stream().mapToInt(c -> c.getData().size()).sum();
        byte[] result = new byte[totalSize];
        int offset = 0;
        for (DerivedChunk chunk : chunks) {
            byte[] data = chunk.getData().toByteArray();
            System.arraycopy(data, 0, result, offset, data.length);
            offset += data.length;
        }
        return result;
    }
}
