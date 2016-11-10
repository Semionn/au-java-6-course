package com.au.mit.torrent.common.protocol;

import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by semionn on 28.10.16.
 */
public class ClientDescription {
    private SocketChannel channel;
    private int localPort;
    private Set<FileDescription> fileDescriptions;

    public ClientDescription(SocketChannel socketChannel) {
        this.channel = socketChannel;
        fileDescriptions = new HashSet<>();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public InetAddress getInetAddress() {
        return channel.socket().getInetAddress();
    }

    public Set<FileDescription> getFileDescriptions() {
        return fileDescriptions;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }
}
