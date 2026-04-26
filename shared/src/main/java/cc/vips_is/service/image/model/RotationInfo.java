package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public record RotationInfo(boolean mirrored, float rotation) {

    public static RotationInfo fromString(String input) {
        log.trace("parse: rotation={}", input);

        if (StringUtils.isBlank(input)) {
            throw new InvalidParameterException("Rotation cannot be null or empty");
        }

        String trimmed = input.trim();
        boolean mirrored = trimmed.startsWith("!");
        String numericPart = mirrored ? trimmed.substring(1) : trimmed;

        if (numericPart.isEmpty()) {
            throw new InvalidParameterException("Rotation value missing after reflection symbol")
                    .addContextValue("input", input);
        }

        try {
            float rawDegrees = Float.parseFloat(numericPart);

            // Normalize degrees to the [0, 360) range
            float normalizedDegrees = rawDegrees % 360;
            if (normalizedDegrees < 0) {
                normalizedDegrees += 360;
            }

            return new RotationInfo(mirrored, normalizedDegrees);

        } catch (NumberFormatException e) {
            throw new InvalidParameterException("Invalid rotation value. Expected a number.")
                    .addContextValue("rotation", numericPart)
                    .addContextValue("input", input);
        }
    }
}

