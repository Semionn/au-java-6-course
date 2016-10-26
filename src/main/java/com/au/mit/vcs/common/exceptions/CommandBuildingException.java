package com.au.mit.vcs.common.exceptions;

/**
 * Class for exceptions occurred during the VCS commands building
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
