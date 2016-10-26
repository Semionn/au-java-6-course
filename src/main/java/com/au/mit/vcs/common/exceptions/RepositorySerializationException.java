package com.au.mit.vcs.common.exceptions;

/**
 * Class for exceptions occurred during the VCS metainfo serialization
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
