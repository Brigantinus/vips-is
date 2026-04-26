package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegionInfoTest {

    @Test
    public void testFromStringFull() {
        RegionInfo regionInfo = RegionInfo.fromString("full");
        assertEquals(RegionType.FULL, regionInfo.type());
        assertEquals(0, regionInfo.x());
        assertEquals(0, regionInfo.y());
        assertEquals(0, regionInfo.width());
        assertEquals(0, regionInfo.height());
    }

    @Test
    public void testFromStringSquare() {
        RegionInfo regionInfo = RegionInfo.fromString("square");
        assertEquals(RegionType.SQUARE, regionInfo.type());
        assertEquals(0, regionInfo.x());
        assertEquals(0, regionInfo.y());
        assertEquals(0, regionInfo.width());
        assertEquals(0, regionInfo.height());
    }

    @Test
    public void testFromStringPixels() {
        RegionInfo regionInfo = RegionInfo.fromString("10,20,100,200");
        assertEquals(RegionType.PIXELS, regionInfo.type());
        assertEquals(10, regionInfo.x());
        assertEquals(20, regionInfo.y());
        assertEquals(100, regionInfo.width());
        assertEquals(200, regionInfo.height());
    }

    @Test
    public void testFromStringPercentage() {
        RegionInfo regionInfo = RegionInfo.fromString("pct:10,20,100,200");
        assertEquals(RegionType.PERCENTAGE, regionInfo.type());
        assertEquals(10, regionInfo.x());
        assertEquals(20, regionInfo.y());
        assertEquals(100, regionInfo.width());
        assertEquals(200, regionInfo.height());
    }

    @Test
    public void testFromStringInvalid() {
        assertThrows(InvalidParameterException.class, () -> RegionInfo.fromString(""));
        assertThrows(InvalidParameterException.class, () -> RegionInfo.fromString("   "));
        assertThrows(InvalidParameterException.class, () -> RegionInfo.fromString("invalid"));
        assertThrows(InvalidParameterException.class, () -> RegionInfo.fromString("10,20,100"));
        assertThrows(InvalidParameterException.class, () -> RegionInfo.fromString("10,20,100,200,300"));
        assertThrows(InvalidParameterException.class, () -> RegionInfo.fromString("10,20,abc,200"));
    }

    @Test
    public void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new RegionInfo(RegionType.PIXELS, -1, 0, 100, 100));
        assertThrows(IllegalArgumentException.class, () -> new RegionInfo(RegionType.PIXELS, 0, -1, 100, 100));
        assertThrows(IllegalArgumentException.class, () -> new RegionInfo(RegionType.PIXELS, 0, 0, -1, 100));
        assertThrows(IllegalArgumentException.class, () -> new RegionInfo(RegionType.PIXELS, 0, 0, 100, -1));
    }
}