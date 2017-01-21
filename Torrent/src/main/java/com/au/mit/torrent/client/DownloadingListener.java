package com.au.mit.torrent.client;

/**
 * Listener interface for updating downloading percentage information
 */
@FunctionalInterface
public interface DownloadingListener {
    void update(TorrentFile torrentFile);
}
