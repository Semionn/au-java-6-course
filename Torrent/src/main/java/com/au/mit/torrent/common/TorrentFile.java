package com.au.mit.torrent.common;

import com.au.mit.torrent.common.protocol.FileDescription;

import java.util.HashSet;
import java.util.Set;

public class TorrentFile {
    private static final int BLOCK_SIZE = 4096;

    private final FileDescription fileDescription;
    private final Set<PeerFileStat> peersStats;

    public TorrentFile(FileDescription fileDescription) {
        this.fileDescription = fileDescription;
        peersStats = new HashSet<>();
    }

    public int getFileID() {
        return fileDescription.getId();
    }

    public void setFileID(int fileID) {
        fileDescription.setId(fileID);
    }
}
