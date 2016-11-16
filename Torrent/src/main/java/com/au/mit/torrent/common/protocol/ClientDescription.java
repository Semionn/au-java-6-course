package com.au.mit.torrent.common.protocol;

import com.au.mit.torrent.common.ClientAddress;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

public class ClientDescription {
    private SocketChannel channel;
    private ClientAddress clientAddress;
    private Set<FileDescription> fileDescriptions;

    public ClientDescription(SocketChannel socketChannel) {
        channel = socketChannel;
        fileDescriptions = new HashSet<>();
        final String host = ((InetSocketAddress) socketChannel.socket().getRemoteSocketAddress()).getAddress().getHostAddress();
        clientAddress = new ClientAddress(host);
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ClientAddress getAddress() {
        return clientAddress;
    }

    public Set<FileDescription> getFileDescriptions() {
        return fileDescriptions;
    }

    public void clearFileDescriptions() {
        fileDescriptions = new HashSet<>();
    }

    public void addFile(FileDescription fileDescription) {
        fileDescriptions.add(fileDescription);
        fileDescription.addSid(this);
    }

    public int getLocalPort() {
        return clientAddress.getPort();
    }

    public void setLocalPort(short localPort) {
        clientAddress.setPort(localPort);
    }
}
