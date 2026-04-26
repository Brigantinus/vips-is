package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SizeInfoTest {

    @Test
    public void testMax() {
        SizeInfo sizeInfo = SizeInfo.max();
        assertEquals(SizeType.MAX, sizeInfo.getType());
        assertNull(sizeInfo.getWidth());
        assertNull(sizeInfo.getHeight());
        assertNull(sizeInfo.getPercentage());
        assertFalse(sizeInfo.isUpscalingAllowed());
        assertTrue(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testMaxUpscale() {
        SizeInfo sizeInfo = SizeInfo.maxUpscale();
        assertEquals(SizeType.MAX, sizeInfo.getType());
        assertNull(sizeInfo.getWidth());
        assertNull(sizeInfo.getHeight());
        assertNull(sizeInfo.getPercentage());
        assertTrue(sizeInfo.isUpscalingAllowed());
        assertTrue(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testWidthOnly() {
        SizeInfo sizeInfo = SizeInfo.widthOnly(100, false);
        assertEquals(SizeType.WIDTH_ONLY, sizeInfo.getType());
        assertEquals(Integer.valueOf(100), sizeInfo.getWidth());
        assertNull(sizeInfo.getHeight());
        assertNull(sizeInfo.getPercentage());
        assertFalse(sizeInfo.isUpscalingAllowed());
        assertTrue(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testHeightOnly() {
        SizeInfo sizeInfo = SizeInfo.heightOnly(200, false);
        assertEquals(SizeType.HEIGHT_ONLY, sizeInfo.getType());
        assertNull(sizeInfo.getWidth());
        assertEquals(Integer.valueOf(200), sizeInfo.getHeight());
        assertNull(sizeInfo.getPercentage());
        assertFalse(sizeInfo.isUpscalingAllowed());
        assertTrue(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testWidthHeight() {
        SizeInfo sizeInfo = SizeInfo.widthHeight(100, 200, true, false);
        assertEquals(SizeType.WIDTH_HEIGHT, sizeInfo.getType());
        assertEquals(Integer.valueOf(100), sizeInfo.getWidth());
        assertEquals(Integer.valueOf(200), sizeInfo.getHeight());
        assertNull(sizeInfo.getPercentage());
        assertTrue(sizeInfo.isUpscalingAllowed());
        assertFalse(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testPercentage() {
        SizeInfo sizeInfo = SizeInfo.percentage(50.0, true);
        assertEquals(SizeType.PERCENTAGE, sizeInfo.getType());
        assertNull(sizeInfo.getWidth());
        assertNull(sizeInfo.getHeight());
        assertEquals(Double.valueOf(50.0), sizeInfo.getPercentage());
        assertTrue(sizeInfo.isUpscalingAllowed());
        assertTrue(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testFromV2StringWithMinusShouldThrowException() {
        assertThrows(InvalidParameterException.class, () -> {
            SizeInfo.fromV2String("-100,");
        });

        assertThrows(InvalidParameterException.class, () -> {
            SizeInfo.fromV2String("100,-50");
        });
    }

    @Test
    public void testFromV2StringMax() {
        SizeInfo sizeInfo = SizeInfo.fromV2String("max");
        assertEquals(SizeType.MAX, sizeInfo.getType());
        assertTrue(sizeInfo.isUpscalingAllowed());
    }

    @Test
    public void testFromV2StringFull() {
        SizeInfo sizeInfo = SizeInfo.fromV2String("full");
        assertEquals(SizeType.MAX, sizeInfo.getType());
        assertFalse(sizeInfo.isUpscalingAllowed());
    }

    @Test
    public void testFromV2StringPercentage() {
        SizeInfo sizeInfo = SizeInfo.fromV2String("pct:50");
        assertEquals(SizeType.PERCENTAGE, sizeInfo.getType());
        assertEquals(Double.valueOf(50.0), sizeInfo.getPercentage());
        assertTrue(sizeInfo.isUpscalingAllowed());
    }

    @Test
    public void testFromV2StringWidthHeight() {
        SizeInfo sizeInfo = SizeInfo.fromV2String("100,200");
        assertEquals(SizeType.WIDTH_HEIGHT, sizeInfo.getType());
        assertEquals(Integer.valueOf(100), sizeInfo.getWidth());
        assertEquals(Integer.valueOf(200), sizeInfo.getHeight());
        assertTrue(sizeInfo.isUpscalingAllowed());
        assertFalse(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testFromV2StringWidthHeightWithAspect() {
        SizeInfo sizeInfo = SizeInfo.fromV2String("!100,200");
        assertEquals(SizeType.WIDTH_HEIGHT, sizeInfo.getType());
        assertEquals(Integer.valueOf(100), sizeInfo.getWidth());
        assertEquals(Integer.valueOf(200), sizeInfo.getHeight());
        assertTrue(sizeInfo.isUpscalingAllowed());
        assertTrue(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testFromV3StringWithMinusShouldThrowException() {
        assertThrows(InvalidParameterException.class, () -> {
            SizeInfo.fromV3String("-100,");
        });

        assertThrows(InvalidParameterException.class, () -> {
            SizeInfo.fromV3String("100,-50");
        });
    }

    @Test
    public void testFromV3StringMax() {
        SizeInfo sizeInfo = SizeInfo.fromV3String("max");
        assertEquals(SizeType.MAX, sizeInfo.getType());
        assertFalse(sizeInfo.isUpscalingAllowed());
    }

    @Test
    public void testFromV3StringMaxUpscale() {
        SizeInfo sizeInfo = SizeInfo.fromV3String("^max");
        assertEquals(SizeType.MAX, sizeInfo.getType());
        assertTrue(sizeInfo.isUpscalingAllowed());
    }

    @Test
    public void testFromV3StringPercentage() {
        SizeInfo sizeInfo = SizeInfo.fromV3String("pct:50");
        assertEquals(SizeType.PERCENTAGE, sizeInfo.getType());
        assertEquals(Double.valueOf(50.0), sizeInfo.getPercentage());
        assertFalse(sizeInfo.isUpscalingAllowed());
    }

    @Test
    public void testFromV3StringPercentageUpscale() {
        SizeInfo sizeInfo = SizeInfo.fromV3String("^pct:50");
        assertEquals(SizeType.PERCENTAGE, sizeInfo.getType());
        assertEquals(Double.valueOf(50.0), sizeInfo.getPercentage());
        assertTrue(sizeInfo.isUpscalingAllowed());
    }

    @Test
    public void testFromV3StringWidthHeight() {
        SizeInfo sizeInfo = SizeInfo.fromV3String("100,200");
        assertEquals(SizeType.WIDTH_HEIGHT, sizeInfo.getType());
        assertEquals(Integer.valueOf(100), sizeInfo.getWidth());
        assertEquals(Integer.valueOf(200), sizeInfo.getHeight());
        assertFalse(sizeInfo.isUpscalingAllowed());
        assertFalse(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testFromV3StringWidthHeightWithAspect() {
        SizeInfo sizeInfo = SizeInfo.fromV3String("!100,200");
        assertEquals(SizeType.WIDTH_HEIGHT, sizeInfo.getType());
        assertEquals(Integer.valueOf(100), sizeInfo.getWidth());
        assertEquals(Integer.valueOf(200), sizeInfo.getHeight());
        assertFalse(sizeInfo.isUpscalingAllowed());
        assertTrue(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testFromV3StringWidthHeightUpscale() {
        SizeInfo sizeInfo = SizeInfo.fromV3String("^100,200");
        assertEquals(SizeType.WIDTH_HEIGHT, sizeInfo.getType());
        assertEquals(Integer.valueOf(100), sizeInfo.getWidth());
        assertEquals(Integer.valueOf(200), sizeInfo.getHeight());
        assertTrue(sizeInfo.isUpscalingAllowed());
        assertFalse(sizeInfo.isMaintainAspectRatio());
    }

    @Test
    public void testFromV2StringInvalid() {
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV2String(""));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV2String("   "));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV2String("invalid"));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV2String("100"));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV2String("100,200,300"));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV2String("100,abc"));
    }

    @Test
    public void testFromV3StringInvalid() {
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV3String(""));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV3String("   "));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV3String("invalid"));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV3String("100"));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV3String("100,200,300"));
        assertThrows(InvalidParameterException.class, () -> SizeInfo.fromV3String("100,abc"));
    }
}