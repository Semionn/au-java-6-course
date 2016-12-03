package com.au.mit.torrent.common.protocol.requests.client;

import java.util.Arrays;

/**
 * Enumerates torrent client request types:
 * Stat - returns info about available parts of the specified file
 * Get - returns file part as byte array
 * Create - handler for forwarding to specialized request handler
 * None - stub for error handling
 */
public enum ClientRequestType {
    CREATE_REQUEST(0),
    STAT(1),
    GET(2),
    NONE(-1);

    private int num;

    ClientRequestType(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    /**
     * Returns ClientRequest type by its number
     */
    public static ClientRequestType getTypeByNum(int num) {
        return Arrays.stream(ClientRequestType.values())
                .filter(t -> t.getNum() == num)
                .findFirst()
                .orElse(NONE);
    }
}
