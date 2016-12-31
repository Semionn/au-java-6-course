package com.au.mit.ftp.common.exceptions;

/**
 * Exception for errors occurred due to unexpected problem at server
 */
public class InternalServerException extends RuntimeException {
    public InternalServerException(String message) {
        super(message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalServerException(Throwable cause) {
        super(cause);
    }
}
