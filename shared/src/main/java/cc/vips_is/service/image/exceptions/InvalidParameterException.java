package cc.vips_is.service.image.exceptions;

import org.apache.commons.lang3.exception.ContextedRuntimeException;

public class InvalidParameterException extends ContextedRuntimeException {

    public InvalidParameterException(String message) {
        super(message);
    }

}
