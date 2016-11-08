package com.au.mit.torrent.common.exceptions;

/**
 * Created by semionn on 08.11.16.
 */
public class CommunicationException extends RuntimeException {
    public CommunicationException() {
    }

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
