package com.au.mit.torrent.client;

public class DownloadingDescription {
    private final PeerDescription peerDescription;
    private final int partNum;

    public DownloadingDescription(PeerDescription peerDescription, int partNum) {
        this.peerDescription = peerDescription;
        this.partNum = partNum;
    }

    public PeerDescription getPeerDescription() {
        return peerDescription;
    }

    public int getPartNum() {
        return partNum;
    }
}
