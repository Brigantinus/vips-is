package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QualityModeTest {

    @Test
    public void testFromStringValidValues() {
        assertEquals(QualityMode.DEFAULT, QualityMode.fromString("color"));
        assertEquals(QualityMode.DEFAULT, QualityMode.fromString("COLOR"));
        assertEquals(QualityMode.GRAY, QualityMode.fromString("gray"));
        assertEquals(QualityMode.BITONAL, QualityMode.fromString("bitonal"));
        assertEquals(QualityMode.DEFAULT, QualityMode.fromString("default"));
    }

    @Test
    public void testFromStringInvalidValue() {
        // Test that invalid values throw an exception
        assertThrows(InvalidParameterException.class, () -> QualityMode.fromString("invalid"));
        assertThrows(InvalidParameterException.class, () -> QualityMode.fromString("invalid-value"));
    }

    @Test
    public void testFromStringBlankValues() {
        // Test that blank values return DEFAULT
        assertEquals(QualityMode.DEFAULT, QualityMode.fromString(""));
        assertEquals(QualityMode.DEFAULT, QualityMode.fromString("   "));
        assertEquals(QualityMode.DEFAULT, QualityMode.fromString(null));
    }

    @Test
    public void testGetLabel() {
        assertEquals("color", QualityMode.COLOR.getLabel());
        assertEquals("gray", QualityMode.GRAY.getLabel());
        assertEquals("bitonal", QualityMode.BITONAL.getLabel());
        assertEquals("default", QualityMode.DEFAULT.getLabel());
    }
}