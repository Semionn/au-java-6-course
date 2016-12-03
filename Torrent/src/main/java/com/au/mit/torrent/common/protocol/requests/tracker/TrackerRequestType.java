package com.au.mit.torrent.common.protocol.requests.tracker;

import java.util.Arrays;

/**
 * Enumerates torrent tracker request types:
 * List - returns info about available files
 * Upload - uploads info about specific file on the tracker
 * Sources - returns info about peers, which distributes a specific file
 * Update - uploads info about distributed files of the peer
 * Create - handler for forwarding to specialized request handler
 * None - stub for error handling
 */
public enum TrackerRequestType {
    CREATE_REQUEST(0),
    LIST(1),
    UPLOAD(2),
    SOURCES(3),
    UPDATE(4),
    NONE(-1);

    private int num;

    TrackerRequestType(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    /**
     * Returns TrackerRequestType type by its number
     */
    public static TrackerRequestType getTypeByNum(int num) {
        return Arrays.stream(TrackerRequestType.values())
                .filter(t -> t.getNum() == num)
                .findFirst()
                .orElse(NONE);
    }
}
