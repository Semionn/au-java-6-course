package com.au.mit.vcs.common.exceptions;

/**
 * Created by semionn on 23.09.16.
 */
public class CommandExecutionException extends RuntimeException {
    public CommandExecutionException(String message) {
        super(message);
    }

    public CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandExecutionException(Throwable cause) {
        super(cause);
    }
}
