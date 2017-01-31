package com.au.mit.torrent.client;

import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.protocol.FileDescription;

/**
 * Wrapper for FileDescription file, which tracks downloading process of it
 */
public class TorrentFile implements java.io.Serializable  {
    private final FileDescription fileDescription;
    private final int totalPartsCount;
    private int downloadedPartsCount;

    /**
     * Constructor
     * Count of downloaded parts will set to total parts count
     * @param fileDescription description of downloading file
     */
    public TorrentFile(FileDescription fileDescription) {
        this(fileDescription, PeerFileStat.getPartsCount(fileDescription.getSize()));
    }

    /**
     * Constructor
     * @param fileDescription description of downloading file
     * @param downloadedPartsCount count of already downloaded parts
     */
    public TorrentFile(FileDescription fileDescription, int downloadedPartsCount) {
        this.fileDescription = fileDescription;
        this.downloadedPartsCount = downloadedPartsCount;
        this.totalPartsCount = PeerFileStat.getPartsCount(fileDescription.getSize());
    }

    /**
     * Increases count of downloaded parts by one, thread-safe
     */
    public synchronized void addPart() {
        if (downloadedPartsCount < totalPartsCount) {
            downloadedPartsCount++;
        }
    }

    /**
     * Returns ratio of downloaded parts to total file parts
     */
    public synchronized double getRatio() {
        return (double) downloadedPartsCount / totalPartsCount;
    }

    public FileDescription getFileDescription() {
        return fileDescription;
    }
    @Override
    public String toString() {
        return "Torrent{" +
                fileDescription +
                ", downloaded=" + (int)(getRatio() * 100) + "%" +
                '}';
    }
}
