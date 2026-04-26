package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public enum QualityMode {
    COLOR("color"),
    GRAY("gray"),
    BITONAL("bitonal"),
    DEFAULT("default");

    private final String label;

    QualityMode(String label) {
        this.label = label;
    }

    private static final Map<String, QualityMode> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(m -> m.label.toLowerCase(), m -> m));

    public static QualityMode fromString(String quality) {
        if (StringUtils.isBlank(quality)) {
            return DEFAULT;
        }

        String normalized = quality.trim().toLowerCase();

        // Handle the specific alias: "color" -> DEFAULT
        if ("color".equals(normalized)) {
            return DEFAULT;
        }

        return Optional.ofNullable(LOOKUP.get(normalized))
                .orElseThrow(() -> new InvalidParameterException("Invalid quality mode.")
                        .addContextValue("quality", quality));
    }
}
