package com.au.mit.ftp.common.exceptions;

/**
 * Exception for errors occurred due to connection problems
 */
public class DisconnectedException extends RuntimeException {
    public DisconnectedException(String message) {
        super(message);
    }

    public DisconnectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DisconnectedException(Throwable cause) {
        super(cause);
    }
}
