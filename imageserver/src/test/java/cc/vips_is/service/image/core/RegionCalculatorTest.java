package cc.vips_is.service.image.core;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.image.model.Region;
import cc.vips_is.service.image.model.RegionInfo;
import cc.vips_is.service.image.model.RegionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegionCalculatorTest {
    
    RegionCalculator regionCalculator = new RegionCalculator();
    
    @Test
    public void testCalculateRegionFull() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.FULL, 0, 0, 0, 0);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Region result = regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);

        // Then
        assertEquals(0, result.left());
        assertEquals(0, result.top());
        assertEquals(imageWidth, result.width());
        assertEquals(imageHeight, result.height());
    }

    @Test
    public void testCalculateRegionSquare() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.SQUARE, 0, 0, 0, 0);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Region result = regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);

        // Then
        assertEquals(100, result.left());  // (800 - 600) / 2
        assertEquals(0, result.top());
        assertEquals(600, result.width());
        assertEquals(600, result.height());
    }

    @Test
    public void testCalculateRegionSquareWithLargerWidth() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.SQUARE, 0, 0, 0, 0);
        int imageWidth = 600;
        int imageHeight = 800;

        // When
        Region result = regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);

        // Then
        assertEquals(0, result.left());
        assertEquals(100, result.top());  // (800 - 600) / 2
        assertEquals(600, result.width());
        assertEquals(600, result.height());
    }

    @Test
    public void testCalculateRegionPixels() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.PIXELS, 100, 50, 300, 200);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Region result = regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);

        // Then
        assertEquals(100, result.left());
        assertEquals(50, result.top());
        assertEquals(300, result.width());
        assertEquals(200, result.height());
    }

    @Test
    public void testCalculateRegionPixelsOutOfBounds() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.PIXELS, 800, 500, 300, 200); // x is exactly at boundary
        int imageWidth = 800;
        int imageHeight = 600;

        // When & Then
        assertThrows(InvalidParameterException.class, () -> {
            regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);
        });
    }

    @Test
    public void testCalculateRegionPixelsOutOfBoundsY() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.PIXELS, 700, 600, 300, 200); // y is exactly at boundary
        int imageWidth = 800;
        int imageHeight = 600;

        // When & Then
        assertThrows(InvalidParameterException.class, () -> {
            regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);
        });
    }

    @Test
    public void testCalculateRegionPixelsOutOfBoundsBeyond() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.PIXELS, 900, 500, 300, 200); // x is beyond boundary
        int imageWidth = 800;
        int imageHeight = 600;

        // When & Then
        assertThrows(InvalidParameterException.class, () -> {
            regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);
        });
    }

    @Test
    public void testCalculateRegionPixelsClamped() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.PIXELS, 100, 50, 800, 600);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Region result = regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);

        // Then
        assertEquals(100, result.left());
        assertEquals(50, result.top());
        assertEquals(700, result.width());  // Clamped to image width - x
        assertEquals(550, result.height()); // Clamped to image height - y
    }

    @Test
    public void testCalculateRegionPercentage() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.PERCENTAGE, 10, 20, 50, 60);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Region result = regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);

        // Then
        assertEquals(80, result.left());   // 10% of 800 = 80
        assertEquals(120, result.top());   // 20% of 600 = 120
        assertEquals(400, result.width()); // 50% of 800 = 400
        assertEquals(360, result.height()); // 60% of 600 = 360
    }

    @Test
    public void testCalculateRegionPercentageWithRounding() {
        // Given
        RegionInfo regionInfo = new RegionInfo(RegionType.PERCENTAGE, 33, 67, 34, 33);
        int imageWidth = 1000;
        int imageHeight = 500;

        // When
        Region result = regionCalculator.calculateRegion(regionInfo, imageWidth, imageHeight);

        // Then
        assertEquals(330, result.left());   // 33% of 1000 = 330
        assertEquals(335, result.top());   // 67% of 500 = 335
        assertEquals(340, result.width()); // 34% of 1000 = 340
        assertEquals(165, result.height()); // 33% of 500 = 165
    }
}