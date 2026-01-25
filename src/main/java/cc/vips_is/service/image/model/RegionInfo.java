package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@Slf4j
public record RegionInfo(RegionType type, int x, int y, int width, int height) {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final String FULL = "full";
    private static final String SQUARE = "square";
    private static final String PCT = "pct:";

    public RegionInfo {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("Region coordinates and dimensions must be non-negative");
        }
    }

    public static RegionInfo fromString(String region) {
        log.trace("fromString: region={}", region);

        if (StringUtils.isBlank(region)) {
            throw new InvalidParameterException("Region must not be null or empty");
        }

        String trimmed = region.trim().toLowerCase();

        return switch (trimmed) {
            case FULL -> new RegionInfo(RegionType.FULL, 0, 0, 0, 0);
            case SQUARE -> new RegionInfo(RegionType.SQUARE, 0, 0, 0, 0);
            default -> parseComplexRegion(trimmed);
        };
    }

    private static RegionInfo parseComplexRegion(String input) {
        RegionType type = RegionType.PIXELS;
        String source = input;

        if (input.startsWith(PCT)) {
            type = RegionType.PERCENTAGE;
            source = input.substring(PCT.length());
        }

        return parseNumericParts(source, type, input);
    }
    private static RegionInfo parseNumericParts(String numericPart, RegionType type, String originalInput) {
        // limit = -1 ensures we don't discard trailing empty strings, allowing better validation
        String[] parts = COMMA_PATTERN.split(numericPart, -1);

        if (parts.length != 4) {
            throw new InvalidParameterException("Region must have exactly 4 comma-separated values (x,y,w,h)")
                    .addContextValue("region", originalInput);
        }

        try {
            return new RegionInfo(
                    type,
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim())
            );
        } catch (NumberFormatException e) {
            throw new InvalidParameterException("Region contains invalid non-numeric values")
                    .addContextValue("region", originalInput);
        }
    }

}
