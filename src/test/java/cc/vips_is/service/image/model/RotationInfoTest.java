package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RotationInfoTest {

    @Test
    public void testFromStringNoMirror() {
        RotationInfo rotationInfo = RotationInfo.fromString("90");
        assertFalse(rotationInfo.mirrored());
        assertEquals(90, rotationInfo.rotation());
    }

    @Test
    public void testFromStringWithMirror() {
        RotationInfo rotationInfo = RotationInfo.fromString("!90");
        assertTrue(rotationInfo.mirrored());
        assertEquals(90, rotationInfo.rotation());
    }

    @Test
    public void testFromStringZeroDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("0");
        assertFalse(rotationInfo.mirrored());
        assertEquals(0, rotationInfo.rotation());
    }

    @Test
    public void testFromStringNegativeDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("-90");
        assertFalse(rotationInfo.mirrored());
        assertEquals(270, rotationInfo.rotation()); // -90 normalized to 270
    }

    @Test
    public void testFromStringLargeDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("450");
        assertFalse(rotationInfo.mirrored());
        assertEquals(90, rotationInfo.rotation()); // 450 normalized to 90
    }

    @Test
    public void testFromStringWithMirrorNegativeDegrees() {
        RotationInfo rotationInfo = RotationInfo.fromString("!-90");
        assertTrue(rotationInfo.mirrored());
        assertEquals(270, rotationInfo.rotation()); // -90 normalized to 270
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