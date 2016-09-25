package com.au.mit.vcs.common.exceptions;

/**
 * Created by semionn on 23.09.16.
 */
public class RepositorySerializationException extends RuntimeException {
    public RepositorySerializationException(String message) {
        super(message);
    }

    public RepositorySerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositorySerializationException(Throwable cause) {
        super(cause);
    }
}
