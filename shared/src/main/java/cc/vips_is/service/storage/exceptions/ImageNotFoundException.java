package cc.vips_is.service.storage.exceptions;

import org.apache.commons.lang3.exception.ContextedRuntimeException;

/**
 * Thrown when the requested image identifier does not exist on disk.
 */
public class ImageNotFoundException extends ContextedRuntimeException {
    public ImageNotFoundException(String message) {
        super(message);
    }
}