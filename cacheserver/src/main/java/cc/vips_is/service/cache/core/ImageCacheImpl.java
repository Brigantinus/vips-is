package cc.vips_is.service.cache.core;

import cc.vips_is.service.cache.CacheStats;
import cc.vips_is.service.cache.DerivedKey;
import cc.vips_is.service.cache.ImageCache;
import cc.vips_is.service.cache.ImageTooLargeForCacheException;
import cc.vips_is.service.image.model.ImageInfo;
import cc.vips_is.service.storage.StorageBackend;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ApplicationScoped
@Slf4j
public class ImageCacheImpl implements ImageCache {

    @ConfigProperty(name = "cache.ttl-hours", defaultValue = "24")
    long ttlHours;

    @ConfigProperty(name = "cache.size-multiplier", defaultValue = "20")
    int sizeMultiplier;

    @Inject
    CacheSizeCalculator sizeCalculator;

    @Inject
    StorageBackend storageBackend;

    private long maxBytes;
    private Duration ttl;

    private final LinkedHashMap<String, CacheEntry> entries = new LinkedHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private long currentBytes = 0;

    private final AtomicLong hits   = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    @PostConstruct
    void init() {
        maxBytes = sizeCalculator.calculate();
        ttl = Duration.ofHours(ttlHours);
        log.info("Cache initialised: maxBytes={}, ttl={}h", maxBytes, ttlHours);
    }

    // ── ImageCache API ───────────────────────────────────────────────────────

    @Override
    public byte[] getOriginal(String identifier) {
        CacheEntry entry = getOrLoad(identifier);
        try {
            return entry.readOriginal();
        } finally {
            entry.close();
        }
    }

    @Override
    public Optional<ImageInfo> getInfo(String identifier) {
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(identifier);
            if (entry != null && !entry.isExpired(ttl) && entry.info != null) {
                hits.incrementAndGet();
                return Optional.of(entry.info);
            }
        } finally {
            lock.readLock().unlock();
        }
        misses.incrementAndGet();
        return Optional.empty();
    }

    @Override
    public void putInfo(String identifier, ImageInfo info) {
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(identifier);
            if (entry != null && !entry.isExpired(ttl)) {
                entry.info = info;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<byte[]> getDerived(String identifier, DerivedKey key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(identifier);
            if (entry == null || entry.isExpired(ttl)) {
                misses.incrementAndGet();
                return Optional.empty();
            }
            byte[] bytes = entry.readDerived(key);
            if (bytes != null) {
                hits.incrementAndGet();
                return Optional.of(bytes);
            }
            misses.incrementAndGet();
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void putDerived(String identifier, DerivedKey key, byte[] bytes) {
        lock.writeLock().lock();
        try {
            CacheEntry entry = entries.get(identifier);
            if (entry == null || entry.isExpired(ttl)) {
                log.trace("putDerived: entry for {} not present, discarding tile", identifier);
                return;
            }
            long added = entry.putDerivedIfAbsent(key, bytes);
            if (added > 0) {
                currentBytes += added;
                evictIfNeeded();
            } else {
                log.trace("putDerived: tile already present for {}, discarding duplicate", identifier);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void evict(String identifier) {
        lock.writeLock().lock();
        try {
            remove(identifier);
            log.info("Evicted cache entry: {}", identifier);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public CacheStats stats() {
        lock.readLock().lock();
        try {
            long total = hits.get() + misses.get();
            double hitRate = total == 0 ? 0.0 : (double) hits.get() / total;
            return new CacheStats(entries.size(), currentBytes, maxBytes, hitRate);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ── internals ────────────────────────────────────────────────────────────

    private CacheEntry getOrLoad(String identifier) {
        // Fast path
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(identifier);
            if (entry != null && !entry.isExpired(ttl)) {
                hits.incrementAndGet();
                return entry.acquire();
            }
        } finally {
            lock.readLock().unlock();
        }

        // Slow path — load from backend under write lock
        lock.writeLock().lock();
        try {
            CacheEntry entry = entries.get(identifier);
            if (entry != null && !entry.isExpired(ttl)) {
                hits.incrementAndGet();
                return entry.acquire();
            }
            if (entry != null) {
                remove(identifier); // expired — evict before reloading
            }

            misses.incrementAndGet();
            byte[] bytes = storageBackend.load(identifier); // I/O under write lock — prevents thundering herd

            long footprint = (long) bytes.length * sizeMultiplier;
            if (footprint > maxBytes) {
                throw new ImageTooLargeForCacheException(identifier, bytes.length, maxBytes, sizeMultiplier);
            }

            CacheEntry newEntry = new CacheEntry(identifier, bytes);
            entries.put(identifier, newEntry);
            currentBytes += bytes.length;
            evictIfNeeded();
            return newEntry.acquire();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** FIFO eviction — removes oldest entries until currentBytes ≤ maxBytes. */
    private void evictIfNeeded() {
        Iterator<Map.Entry<String, CacheEntry>> it = entries.entrySet().iterator();
        while (currentBytes > maxBytes && it.hasNext()) {
            CacheEntry victim = it.next().getValue();
            currentBytes -= victim.totalBytes();
            it.remove();
            victim.close(); // release cache's ref; arena closes when all readers finish
            log.debug("Evicted (size limit): {}", victim.identifier);
        }
    }

    private void remove(String identifier) {
        CacheEntry removed = entries.remove(identifier);
        if (removed != null) {
            currentBytes -= removed.totalBytes();
            removed.close();
        }
    }

    @Scheduled(every = "1h")
    void sweepExpired() {
        lock.writeLock().lock();
        try {
            int before = entries.size();
            entries.entrySet().removeIf(e -> {
                if (e.getValue().isExpired(ttl)) {
                    currentBytes -= e.getValue().totalBytes();
                    e.getValue().close();
                    return true;
                }
                return false;
            });
            int removed = before - entries.size();
            if (removed > 0) {
                log.info("TTL sweep removed {} expired cache entries", removed);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
