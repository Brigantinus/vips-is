package cc.vips_is.service.image.core;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.image.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SizeCalculatorTest {

    SizeCalculator sizeCalculator = new SizeCalculator();

    @Test
    public void testCalculateSourceRegionFull() {
        // Given
        RegionInfo region = new RegionInfo(RegionType.FULL, 0, 0, 0, 0);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Size result = sizeCalculator.calculateSourceRegion(region, imageWidth, imageHeight);

        // Then
        assertEquals(imageWidth, result.width());
        assertEquals(imageHeight, result.height());
    }

    @Test
    public void testCalculateSourceRegionSquare() {
        // Given
        RegionInfo region = new RegionInfo(RegionType.SQUARE, 0, 0, 0, 0);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Size result = sizeCalculator.calculateSourceRegion(region, imageWidth, imageHeight);

        // Then
        assertEquals(600, result.width());  // min(800, 600)
        assertEquals(600, result.height()); // min(800, 600)
    }

    @Test
    public void testCalculateSourceRegionSquareWithLargerWidth() {
        // Given
        RegionInfo region = new RegionInfo(RegionType.SQUARE, 0, 0, 0, 0);
        int imageWidth = 600;
        int imageHeight = 800;

        // When
        Size result = sizeCalculator.calculateSourceRegion(region, imageWidth, imageHeight);

        // Then
        assertEquals(600, result.width());  // min(600, 800)
        assertEquals(600, result.height()); // min(600, 800)
    }

    @Test
    public void testCalculateSourceRegionPixels() {
        // Given
        RegionInfo region = new RegionInfo(RegionType.PIXELS, 100, 50, 300, 200);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Size result = sizeCalculator.calculateSourceRegion(region, imageWidth, imageHeight);

        // Then
        assertEquals(300, result.width());
        assertEquals(200, result.height());
    }

    @Test
    public void testCalculateSourceRegionPercentage() {
        // Given
        RegionInfo region = new RegionInfo(RegionType.PERCENTAGE, 10, 20, 50, 60);
        int imageWidth = 800;
        int imageHeight = 600;

        // When
        Size result = sizeCalculator.calculateSourceRegion(region, imageWidth, imageHeight);

        // Then
        assertEquals(400, result.width());  // 50% of 800 = 400
        assertEquals(360, result.height()); // 60% of 600 = 360
    }

    @Test
    public void testCalculateScaledSizesMaxNoUpscaling() {
        // Given
        SizeInfo size = SizeInfo.max();
        Size source = new Size(800, 600);
        int maxWidth = 1000;
        int maxHeight = 800;
        long maxArea = 1000000;

        // When
        Size result = sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);

        // Then
        assertEquals(800, result.width());
        assertEquals(600, result.height());
    }

    @Test
    public void testCalculateScaledSizesMaxUpscalingAllowed() {
        // Given
        SizeInfo size = SizeInfo.maxUpscale();
        Size source = new Size(800, 600);
        int maxWidth = 100;
        int maxHeight = 100;
        long maxArea = 1000000;

        // When
        Size result = sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);

        // Then
        assertEquals(100, result.width());
        assertEquals(75, result.height()); // Scaled to fit within max dimensions
    }

    @Test
    public void testCalculateScaledSizesWidthOnly() {
        // Given
        SizeInfo size = SizeInfo.widthOnly(400);
        Size source = new Size(800, 600);
        int maxWidth = 1000;
        int maxHeight = 800;
        long maxArea = 1000000;

        // When
        Size result = sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);

        // Then
        assertEquals(400, result.width());
        assertEquals(300, result.height()); // 600 * (400/800) = 300
    }

    @Test
    public void testCalculateScaledSizesHeightOnly() {
        // Given
        SizeInfo size = SizeInfo.heightOnly(300);
        Size source = new Size(800, 600);
        int maxWidth = 1000;
        int maxHeight = 800;
        long maxArea = 1000000;

        // When
        Size result = sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);

        // Then
        assertEquals(400, result.width()); // 800 * (300/600) = 400
        assertEquals(300, result.height());
    }

    @Test
    public void testCalculateScaledSizesPercentage() {
        // Given
        SizeInfo size = SizeInfo.percentage(50.0, false);
        Size source = new Size(800, 600);
        int maxWidth = 1000;
        int maxHeight = 800;
        long maxArea = 1000000;

        // When
        Size result = sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);

        // Then
        assertEquals(400, result.width());  // 800 * 0.5 = 400
        assertEquals(300, result.height()); // 600 * 0.5 = 300
    }

    @Test
    public void testCalculateScaledSizesWidthHeightMaintainAspect() {
        // Given
        SizeInfo size = SizeInfo.widthHeight(400, 300, false, true);
        Size source = new Size(800, 600);
        int maxWidth = 1000;
        int maxHeight = 800;
        long maxArea = 1000000;

        // When
        Size result = sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);

        // Then
        assertEquals(400, result.width());
        assertEquals(300, result.height()); // Maintains aspect ratio
    }

    @Test
    public void testCalculateScaledSizesWidthHeightNoAspect() {
        // Given
        SizeInfo size = SizeInfo.widthHeight(400, 300, false, false);
        Size source = new Size(800, 600);
        int maxWidth = 1000;
        int maxHeight = 800;
        long maxArea = 1000000;

        // When
        Size result = sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);

        // Then
        assertEquals(400, result.width());
        assertEquals(300, result.height()); // Forced sizes
    }

    @Test
    public void testCalculateScaledSizesUpscalingNotAllowed() {
        // Given
        SizeInfo size = SizeInfo.widthHeight(1000, 800, false, true);
        Size source = new Size(800, 600);
        int maxWidth = 1000;
        int maxHeight = 800;
        long maxArea = 1000000;

        // When & Then
        assertThrows(InvalidParameterException.class, () -> {
            sizeCalculator.calculateScaledSizes(size, source, maxWidth, maxHeight, maxArea);
        });
    }

    @Test
    public void testCalculateRotationBoundingBoxNoRotation() {
        // Given
        Size current = new Size(800, 600);
        RotationInfo rotation = new RotationInfo(false, 0.0f);

        // When
        Size result = sizeCalculator.calculateRotationBoundingBox(current, rotation);

        // Then
        assertEquals(800, result.width());
        assertEquals(600, result.height());
    }

    @Test
    public void testCalculateRotationBoundingBox90Degrees() {
        // Given
        Size current = new Size(800, 600);
        RotationInfo rotation = new RotationInfo(false, 90.0f);

        // When
        Size result = sizeCalculator.calculateRotationBoundingBox(current, rotation);

        // Then
        assertEquals(600, result.width());  // Width and height swapped for 90 degree rotation
        assertEquals(800, result.height());
    }

    @Test
    public void testCalculateRotationBoundingBox270Degrees() {
        // Given
        Size current = new Size(800, 600);
        RotationInfo rotation = new RotationInfo(false, 270.0f);

        // When
        Size result = sizeCalculator.calculateRotationBoundingBox(current, rotation);

        // Then
        assertEquals(600, result.width());  // Width and height swapped for 270 degree rotation
        assertEquals(800, result.height());
    }

    @Test
    public void testCalculateRotationBoundingBox45Degrees() {
        // Given
        Size current = new Size(800, 600);
        RotationInfo rotation = new RotationInfo(false, 45.0f);

        // When
        Size result = sizeCalculator.calculateRotationBoundingBox(current, rotation);

        // Then
        assertEquals(990, result.width());  // Calculated using rotation formula
        assertEquals(990, result.height()); // Should be approximately same due to square result
    }
}
