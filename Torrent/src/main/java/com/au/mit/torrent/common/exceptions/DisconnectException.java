package com.au.mit.torrent.common.exceptions;

public class DisconnectException extends RuntimeException {
    public DisconnectException() {
    }

    public DisconnectException(String message) {
        super(message);
    }
}
