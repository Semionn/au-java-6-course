package com.au.mit.vcs.common.exceptions;

/**
 * Thrown when command constructor received not enough arguments for the command execution
 */
public class NotEnoughArgumentsException extends CommandBuildingException {
    public NotEnoughArgumentsException(String message) {
        super(message);
    }

    public NotEnoughArgumentsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughArgumentsException(Throwable cause) {
        super(cause);
    }
}
