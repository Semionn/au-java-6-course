package com.au.mit.torrent.common.exceptions;

/**
 * Created by semionn on 10.11.16.
 */
public class RequestException extends RuntimeException {
    public RequestException() {
    }

    public RequestException(String message) {
        super(message);
    }
}
