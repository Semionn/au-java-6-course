package com.au.mit.torrent.client;

import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.PeerFileStat;

/**
 * Data class for storing pair: peer address - file stat info
 */
public class PeerDescription {
    private final ClientAddress peerAddress;
    private final PeerFileStat peerFileStat;

    public PeerDescription(ClientAddress peerAddress, PeerFileStat peerFileStat) {
        this.peerAddress = peerAddress;
        this.peerFileStat = peerFileStat;
    }

    public ClientAddress getPeerAddress() {
        return peerAddress;
    }

    public PeerFileStat getPeerFileStat() {
        return peerFileStat;
    }
}
