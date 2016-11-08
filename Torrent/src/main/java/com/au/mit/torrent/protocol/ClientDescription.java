package com.au.mit.torrent.protocol;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by semionn on 28.10.16.
 */
public class ClientDescription {
    private InetAddress inetAddress;
    private Set<FileDescription> fileDescriptions;

    public ClientDescription(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        fileDescriptions = new HashSet<>();
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public Set<FileDescription> getFileDescriptions() {
        return fileDescriptions;
    }
}
