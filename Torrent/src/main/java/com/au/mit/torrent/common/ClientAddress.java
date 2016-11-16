package com.au.mit.torrent.common;

import java.util.Objects;

public class ClientAddress {
    private String hostIP;
    private short port;

    public ClientAddress(String hostIP) {
        this(hostIP, (short) -1);
    }

    public ClientAddress(String hostIP, short port) {
        this.hostIP = hostIP;
        this.port = port;
    }

    public String getHostIP() {
        return hostIP;
    }

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public byte getIPByte(byte num) {
        return Byte.valueOf(hostIP.split("\\.")[num]);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientAddress)) {
            return false;
        }
        return Objects.equals(hostIP, ((ClientAddress) obj).hostIP) && port == ((ClientAddress) obj).port;
    }

    @Override
    public int hashCode() {
        return hostIP.hashCode() * 3 + Integer.hashCode(port);
    }
}
