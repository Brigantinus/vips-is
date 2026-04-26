package cc.vips_is.service.cache.core;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

@ApplicationScoped
@Slf4j
public class CacheSizeCalculator {

    @ConfigProperty(name = "cache.max-images", defaultValue = "10")
    int maxImages;

    @ConfigProperty(name = "cache.estimated-image-bytes", defaultValue = "524288000") // 500 MB
    long estimatedImageBytes;

    @ConfigProperty(name = "cache.memory-overhead-bytes", defaultValue = "536870912") // 512 MB
    long memoryOverheadBytes;

    public long calculate() {
        long containerLimit = readContainerMemoryLimit().orElseGet(this::totalSystemMemory);
        long jvmHeap = Runtime.getRuntime().maxMemory();
        long availableForCache = containerLimit - jvmHeap - memoryOverheadBytes;
        long cap = (long) maxImages * estimatedImageBytes;
        long result = Math.min(availableForCache, cap);

        if (result < estimatedImageBytes) {
            throw new IllegalStateException(String.format(
                    "Calculated cache size (%s) is below the estimated single-image size (%s). " +
                    "Increase container memory or reduce cache.estimated-image-bytes.",
                    humanReadable(result), humanReadable(estimatedImageBytes)));
        }

        log.info("Cache auto-sized: containerLimit={}, jvmHeap={}, overhead={}, cap={}×{}={}, result={}",
                humanReadable(containerLimit), humanReadable(jvmHeap), humanReadable(memoryOverheadBytes),
                maxImages, humanReadable(estimatedImageBytes), humanReadable(cap), humanReadable(result));

        return result;
    }

    private OptionalLong readContainerMemoryLimit() {
        // cgroup v2
        OptionalLong v2 = readCgroupFile(Path.of("/sys/fs/cgroup/memory.max"));
        if (v2.isPresent()) return v2;
        // cgroup v1
        return readCgroupFile(Path.of("/sys/fs/cgroup/memory/memory.limit_in_bytes"));
    }

    private OptionalLong readCgroupFile(Path path) {
        try {
            if (!Files.exists(path)) return OptionalLong.empty();
            String content = Files.readString(path).strip();
            if ("max".equalsIgnoreCase(content)) return OptionalLong.empty(); // no limit set
            return OptionalLong.of(Long.parseLong(content));
        } catch (IOException | NumberFormatException e) {
            log.debug("Could not read cgroup file {}: {}", path, e.getMessage());
            return OptionalLong.empty();
        }
    }

    private long totalSystemMemory() {
        // Fallback: use a large value so the cap (maxImages × estimatedImageBytes) governs
        log.info("No container memory limit detected; using max-images cap only");
        return (long) maxImages * estimatedImageBytes + Runtime.getRuntime().maxMemory() + memoryOverheadBytes;
    }

    private static String humanReadable(long bytes) {
        if (bytes >= 1L << 30) return String.format("%.1f GB", bytes / (double) (1L << 30));
        if (bytes >= 1L << 20) return String.format("%.1f MB", bytes / (double) (1L << 20));
        return bytes + " B";
    }
}
