package com.au.mit.vcs.common.exceptions;

/**
 * Thrown when the parsed command name not corresponds any known VCS command
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
