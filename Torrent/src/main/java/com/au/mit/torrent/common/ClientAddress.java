package com.au.mit.torrent.common;

import java.util.Objects;

/**
 * Data class for storing tracker client address
 */
public class ClientAddress {
    private final String hostIP;
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
        return (byte)(Integer.valueOf(hostIP.split("\\.")[num])-128);
    }

    public static int IPByteToInt(byte b) {
        return (int) b + 128;
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
