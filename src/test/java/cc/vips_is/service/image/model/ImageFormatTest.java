package cc.vips_is.service.image.model;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImageFormatTest {

    @Test
    public void testFromStringValidExtensions() {
        assertEquals(ImageFormat.JPG, ImageFormat.fromString("jpg"));
        assertEquals(ImageFormat.JPG, ImageFormat.fromString("JPG"));
        assertEquals(ImageFormat.JPG, ImageFormat.fromString(".jpg"));
        assertEquals(ImageFormat.PNG, ImageFormat.fromString("png"));
        assertEquals(ImageFormat.TIF, ImageFormat.fromString("tif"));
        assertEquals(ImageFormat.TIF, ImageFormat.fromString("tiff"));
        assertEquals(ImageFormat.GIF, ImageFormat.fromString("gif"));
        assertEquals(ImageFormat.JP2, ImageFormat.fromString("jp2"));
        assertEquals(ImageFormat.WEBP, ImageFormat.fromString("webp"));
    }

    @Test
    public void testFromStringInvalidExtension() {
        assertThrows(InvalidParameterException.class, () -> ImageFormat.fromString("invalid"));
        assertThrows(InvalidParameterException.class, () -> ImageFormat.fromString(""));
        assertThrows(InvalidParameterException.class, () -> ImageFormat.fromString("   "));
    }

    @Test
    public void testGetMimeType() {
        assertEquals("image/jpeg", ImageFormat.JPG.getMimeType());
        assertEquals("image/png", ImageFormat.PNG.getMimeType());
        assertEquals("image/tiff", ImageFormat.TIF.getMimeType());
        assertEquals("image/gif", ImageFormat.GIF.getMimeType());
        assertEquals("image/jp2", ImageFormat.JP2.getMimeType());
        assertEquals("image/webp", ImageFormat.WEBP.getMimeType());
    }

    @Test
    public void testGetExtension() {
        assertEquals("jpg", ImageFormat.JPG.getExtension());
        assertEquals("png", ImageFormat.PNG.getExtension());
        assertEquals("tif", ImageFormat.TIF.getExtension());
        assertEquals("gif", ImageFormat.GIF.getExtension());
        assertEquals("jp2", ImageFormat.JP2.getExtension());
        assertEquals("webp", ImageFormat.WEBP.getExtension());
    }
}