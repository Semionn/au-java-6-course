package com.au.mit.torrent.common.protocol.requests.client;

/**
 * Created by semionn on 09.11.16.
 */
public enum ClientRequestType {
    STAT(1),
    GET(2);

    private int num;

    ClientRequestType(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }
}
