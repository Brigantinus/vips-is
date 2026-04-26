package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RotationInfoTest {

    @Test
    public void testFromStringNoMirror() {
        RotationInfo rotationInfo = RotationInfo.fromString("90");
        assertFalse(rotationInfo.mirrored());
        assertEquals(90.0f, rotationInfo.rotation());
    }

    @Test
    public void testFromStringWithMirror() {
        RotationInfo rotationInfo = RotationInfo.fromString("!90");
        assertTrue(rotationInfo.mirrored());
        assertEquals(90.0f, rotationInfo.rotation());
    }

    @Test
    public void testFromStringZeroDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("0");
        assertFalse(rotationInfo.mirrored());
        assertEquals(0.0f, rotationInfo.rotation());
    }

    @Test
    public void testFromStringNegativeDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("-90");
        assertFalse(rotationInfo.mirrored());
        assertEquals(270.0f, rotationInfo.rotation()); // -90 normalized to 270
    }

    @Test
    public void testFromStringLargeDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("540");
        assertFalse(rotationInfo.mirrored());
        assertEquals(180.0f, rotationInfo.rotation()); // 540 normalized to 180
    }

    @Test
    public void testFromStringWithMirrorNegativeDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("!-90");
        assertTrue(rotationInfo.mirrored());
        assertEquals(270.0f, rotationInfo.rotation()); // -90 normalized to 270
    }

    @Test
    public void testFromStringInvalid() {
        assertThrows(InvalidParameterException.class, () -> RotationInfo.fromString(""));
        assertThrows(InvalidParameterException.class, () -> RotationInfo.fromString("   "));
        assertThrows(InvalidParameterException.class, () -> RotationInfo.fromString("invalid"));
        assertThrows(InvalidParameterException.class, () -> RotationInfo.fromString("!"));
        assertThrows(InvalidParameterException.class, () -> RotationInfo.fromString("abc"));
    }
}
