package com.au.mit.torrent.client;

import com.au.mit.torrent.common.protocol.FileDescription;

/**
 * Listener interface for updating downloading percentage information
 */
@FunctionalInterface
public interface DownloadingListener {
    void update(FileDescription fileDescription);
}
