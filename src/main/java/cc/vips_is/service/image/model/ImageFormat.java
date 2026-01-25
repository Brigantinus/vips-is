package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ImageFormat {
    JPG("image/jpeg", "jpg"),
    TIF("image/tiff", "tif"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    JP2("image/jp2", "jp2"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;

    private static final Map<String, ImageFormat> EXTENSION_MAP = new HashMap<>();

    static {
        for (ImageFormat format : values()) {
            EXTENSION_MAP.put(format.extension.toLowerCase(), format);
        }
        // Register common aliases
        EXTENSION_MAP.put("jpeg", JPG);
        EXTENSION_MAP.put("tiff", TIF);
    }

    /**
     * Centralized parsing logic within the Enum itself.
     */
    public static ImageFormat fromString(String extension) {
        log.trace("Parsing extension: {}", extension);

        if (StringUtils.isBlank(extension)) {
            throw new InvalidParameterException("Extension cannot be null or empty");
        }

        String normalized = extension.toLowerCase().replace(".", "").trim();

        return Optional.ofNullable(EXTENSION_MAP.get(normalized))
                .orElseThrow(() -> new InvalidParameterException("Unsupported image format.")
                        .addContextValue("extension", extension));
    }
}