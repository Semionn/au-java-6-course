package com.au.mit.vcs.common.exceptions;

/**
 * Created by semionn on 23.09.16.
 */
public class CommandBuildingException extends Exception {
    public CommandBuildingException(String message) {
        super(message);
    }

    public CommandBuildingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandBuildingException(Throwable cause) {
        super(cause);
    }
}
