package com.au.mit.torrent.common.protocol.requests.tracker;

import java.util.Arrays;

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

    public static TrackerRequestType getTypeByNum(int num) {
        return Arrays.stream(TrackerRequestType.values())
                .filter(t -> t.getNum() == num)
                .findFirst()
                .orElse(NONE);
    }
}
