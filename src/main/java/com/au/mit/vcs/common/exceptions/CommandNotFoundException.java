package com.au.mit.vcs.common.exceptions;

/**
 * Created by semionn on 23.09.16.
 */
public class CommandNotFoundException extends CommandBuildingException {
    public CommandNotFoundException(String message) {
        super(message);
    }

    public CommandNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandNotFoundException(Throwable cause) {
        super(cause);
    }
}
