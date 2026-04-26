package cc.vips_is.service.storage.exceptions;

import org.apache.commons.lang3.exception.ContextedRuntimeException;

public class StorageException extends ContextedRuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
