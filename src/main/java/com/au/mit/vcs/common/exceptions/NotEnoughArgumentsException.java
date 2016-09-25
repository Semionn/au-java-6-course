package com.au.mit.vcs.common.exceptions;

/**
 * Created by semionn on 23.09.16.
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
