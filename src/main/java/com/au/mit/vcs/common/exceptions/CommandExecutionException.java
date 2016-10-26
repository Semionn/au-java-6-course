package com.au.mit.vcs.common.exceptions;

/**
 * Class for exceptions occurred during the VCS commands execution
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
