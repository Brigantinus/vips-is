package cc.vips_is.service.image.exceptions;

import org.apache.commons.lang3.exception.ContextedRuntimeException;

public class ImageProcessingException extends ContextedRuntimeException {
    public ImageProcessingException(String message) {
        super(message);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
