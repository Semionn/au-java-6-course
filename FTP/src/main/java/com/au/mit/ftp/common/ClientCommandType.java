package com.au.mit.ftp.common;

import java.util.Objects;

/**
 * Types of command, allowed to proceed by the FTP client.
 * Each command have name for the client app command line
 */
public enum ClientCommandType {
    CONNECT("connect"),
    DISCONNECT("disconnect");

    private final String name;

    ClientCommandType(String name) {
        this.name = name;
    }

    public static ClientCommandType getByName(String name) {
        for (ClientCommandType cmd : values()) {
            if (Objects.equals(cmd.name, name)) {
                return cmd;
            }
        }
        return null;
    }
}
