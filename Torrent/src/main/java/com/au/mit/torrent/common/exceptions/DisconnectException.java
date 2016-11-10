package com.au.mit.torrent.common.exceptions;

/**
 * Created by semionn on 10.11.16.
 */
public class DisconnectException extends RuntimeException {
    public DisconnectException() {
    }

    public DisconnectException(String message) {
        super(message);
    }
}
