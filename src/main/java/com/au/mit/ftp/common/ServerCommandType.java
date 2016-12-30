package com.au.mit.ftp.common;

import java.util.Objects;

/**
 * Types of command, allowed to proceed by the FTP server.
 * Each command have an id for network transferring and name for the client app command line
 */
public enum ServerCommandType {
    LIST(1, "list"),
    GET(2, "get");

    private final int id;
    private final String name;

    ServerCommandType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static ServerCommandType getByName(String name) {
        for (ServerCommandType cmd : values()) {
            if (Objects.equals(cmd.name, name)) {
                return cmd;
            }
        }
        return null;
    }

    public static ServerCommandType getByID(int id) {
        for (ServerCommandType cmd : values()) {
            if (cmd.id == id) {
                return cmd;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }
}
