package com.au.mit.ftp.common.exceptions;

/**
 * Exception for errors occurred due to connection problems
 */
public class CommunicationException extends RuntimeException {

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommunicationException(Throwable cause) {
        super(cause);
    }
}
