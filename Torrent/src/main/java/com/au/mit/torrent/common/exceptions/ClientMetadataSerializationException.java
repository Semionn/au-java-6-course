package com.au.mit.torrent.common.exceptions;

public class ClientMetadataSerializationException extends RuntimeException {
    public ClientMetadataSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientMetadataSerializationException(Throwable e) {
    }
}
