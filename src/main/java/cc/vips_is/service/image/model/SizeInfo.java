package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Getter
public class SizeInfo {

    private static final String UPSCALE_PREFIX = "^";
    private static final String ASPECT_RATIO_PREFIX = "!";
    private static final String MAX = "max";
    private static final String FULL = "full";
    private static final String COMMA = ",";
    private static final String PCT_PREFIX = "pct:";

    private SizeType type;
    private Integer width;
    private Integer height;
    private Double percentage;
    private boolean upscalingAllowed;
    private boolean maintainAspectRatio;

    public static SizeInfo max() {
        return new SizeInfo(SizeType.MAX, null, null, null, false, true);
    }

    public static SizeInfo maxUpscale() {
        return new SizeInfo(SizeType.MAX, null, null, null, true, true);
    }

    public static SizeInfo widthOnly(int width) {
        return new SizeInfo(SizeType.WIDTH_ONLY, width, null, null, false, true);
    }

    public static SizeInfo heightOnly(int height) {
        return new SizeInfo(SizeType.HEIGHT_ONLY, null, height, null, false, true);
    }

    public static SizeInfo widthHeight(int width, int height, boolean upscalingAllowed, boolean maintainAspectRatio) {
        return new SizeInfo(SizeType.WIDTH_HEIGHT, width, height, null, upscalingAllowed, maintainAspectRatio);
    }

    public static SizeInfo percentage(double percentage, boolean upscalingAllowed) {
        return new SizeInfo(SizeType.PERCENTAGE, null, null, percentage, upscalingAllowed, true);
    }

    /**
     * Parses a IIIF Image API v2.1 size string.
     */
    public static SizeInfo fromV2String(String size) {
        if (StringUtils.isBlank(size)) {
            throw new InvalidParameterException("Size parameter cannot be null or empty");
        }

        String cleaned = size.trim();

        if (MAX.equals(cleaned)) return SizeInfo.maxUpscale();
        if (FULL.equals(cleaned)) return SizeInfo.max();

        if (cleaned.startsWith(PCT_PREFIX)) {
            return parsePercentage(cleaned, true);
        }

        boolean maintainAspectRatio = cleaned.contains(ASPECT_RATIO_PREFIX);
        cleaned = cleaned.replace(ASPECT_RATIO_PREFIX, "");

        return parseDimensions(cleaned, true, maintainAspectRatio);
    }

    /**
     * Parses a IIIF Image API v3.0 size string.
     */
    public static SizeInfo fromV3String(String size) {
        if (StringUtils.isBlank(size)) {
            throw new InvalidParameterException("Size parameter cannot be null or empty");
        }

        boolean upscalingAllowed = size.startsWith(UPSCALE_PREFIX);
        String cleaned = upscalingAllowed ? size.substring(1) : size;

        boolean maintainAspectRatio = cleaned.contains(ASPECT_RATIO_PREFIX);
        cleaned = cleaned.replace(ASPECT_RATIO_PREFIX, "");

        if (MAX.equals(cleaned)) {
            return upscalingAllowed ? SizeInfo.maxUpscale() : SizeInfo.max();
        }

        if (cleaned.startsWith(PCT_PREFIX)) {
            return parsePercentage(cleaned, upscalingAllowed);
        }

        return parseDimensions(cleaned, upscalingAllowed, maintainAspectRatio);
    }

    // --- Shared Private Helpers ---

    private static SizeInfo parsePercentage(String input, boolean upscalingAllowed) {
        try {
            double dPercentage = Double.parseDouble(input.substring(PCT_PREFIX.length()));
            return SizeInfo.percentage(dPercentage, upscalingAllowed);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException("Invalid percentage for size.")
                    .addContextValue("input", input);
        }
    }

    private static SizeInfo parseDimensions(String dimensions, boolean upscale, boolean keepAspect) {
        String[] parts = dimensions.split(COMMA, -1);

        if (parts.length != 2) {
            throw new InvalidParameterException("Invalid dimensions format.")
                    .addContextValue("dimensions", dimensions);
        }

        Integer width = parseSingleDimension(parts[0].trim(), "width");
        Integer height = parseSingleDimension(parts[1].trim(), "height");

        if (width == null && height == null) {
            throw new InvalidParameterException("Width and height cannot both be empty.");
        }

        if (width == null) return SizeInfo.heightOnly(height);
        if (height == null) return SizeInfo.widthOnly(width);

        return SizeInfo.widthHeight(width, height, upscale, keepAspect);
    }

    private static Integer parseSingleDimension(String part, String label) {
        if (part.isEmpty()) return null;
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException("Invalid " + label).addContextValue(label, part);
        }
    }
}

