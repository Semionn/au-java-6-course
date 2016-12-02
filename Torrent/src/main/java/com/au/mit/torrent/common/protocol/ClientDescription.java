package com.au.mit.torrent.common.protocol;

import com.au.mit.torrent.common.ClientAddress;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class ClientDescription {
    private SocketChannel channel;
    private ClientAddress clientAddress;
    private Set<FileDescription> fileDescriptions;
    private LocalDateTime lastAccessTime = LocalDateTime.now();

    public ClientDescription(SocketChannel socketChannel) {
        this(socketChannel,
                new ClientAddress(
                        ((InetSocketAddress) socketChannel.socket().getRemoteSocketAddress())
                                .getAddress()
                                .getHostAddress()
                )
        );
    }

    public ClientDescription(SocketChannel socketChannel, ClientAddress clientAddress) {
        channel = socketChannel;
        fileDescriptions = new HashSet<>();
        this.clientAddress = clientAddress;
    }

    public void updateAccessTime() {
        lastAccessTime = LocalDateTime.now();
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
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
