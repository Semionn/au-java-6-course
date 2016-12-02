package com.au.mit.torrent.common.protocol.requests.client;

import java.util.Arrays;

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

    public static ClientRequestType getTypeByNum(int num) {
        return Arrays.stream(ClientRequestType.values())
                .filter(t -> t.getNum() == num)
                .findFirst()
                .orElse(NONE);
    }
}
