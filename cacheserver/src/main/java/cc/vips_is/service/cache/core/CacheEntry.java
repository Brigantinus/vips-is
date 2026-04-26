package cc.vips_is.service.cache.core;

import cc.vips_is.service.cache.DerivedKey;
import cc.vips_is.service.image.model.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class CacheEntry implements AutoCloseable {

    final String identifier;
    final Arena arena;
    final MemorySegment originalSegment;
    volatile ImageInfo info;

    private final ConcurrentHashMap<DerivedKey, MemorySegment> derived = new ConcurrentHashMap<>();
    private final Instant loadedAt = Instant.now();
    private final AtomicInteger refCount = new AtomicInteger(1); // 1 = cache holds a ref

    CacheEntry(String identifier, byte[] bytes) {
        this.identifier = identifier;
        this.arena = Arena.ofShared();
        this.originalSegment = arena.allocate(bytes.length);
        this.originalSegment.copyFrom(MemorySegment.ofArray(bytes));
    }

    CacheEntry acquire() {
        refCount.incrementAndGet();
        return this;
    }

    @Override
    public void close() {
        if (refCount.decrementAndGet() == 0) {
            arena.close();
            log.trace("Arena closed for {}", identifier);
        }
    }

    /** Returns bytes added, or 0 if the key was already present (first-write-wins). */
    long putDerivedIfAbsent(DerivedKey key, byte[] bytes) {
        if (derived.containsKey(key)) return 0L;
        MemorySegment seg = arena.allocate(bytes.length);
        seg.copyFrom(MemorySegment.ofArray(bytes));
        if (derived.putIfAbsent(key, seg) != null) {
            // Lost a race — segment is allocated but unreachable; cleaned on arena.close()
            return 0L;
        }
        return bytes.length;
    }

    byte[] readOriginal() {
        return originalSegment.toArray(ValueLayout.JAVA_BYTE);
    }

    byte[] readDerived(DerivedKey key) {
        MemorySegment seg = derived.get(key);
        return seg == null ? null : seg.toArray(ValueLayout.JAVA_BYTE);
    }

    boolean hasDerived(DerivedKey key) {
        return derived.containsKey(key);
    }

    boolean isExpired(Duration ttl) {
        return Duration.between(loadedAt, Instant.now()).compareTo(ttl) > 0;
    }

    long totalBytes() {
        return originalSegment.byteSize()
                + derived.values().stream().mapToLong(MemorySegment::byteSize).sum();
    }
}
