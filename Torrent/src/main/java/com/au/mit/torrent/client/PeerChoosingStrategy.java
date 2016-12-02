package com.au.mit.torrent.client;

import java.util.Set;

/**
 * Interface to find mapping from parts of downloading file to certain peers
 */
@FunctionalInterface
public interface PeerChoosingStrategy {

    /**
     * Evaluating set of pairs (file part - peer) to download (file)
     * @param localParts set of already downloaded parts numbers
     * @param peerDescriptions set of stat info for peers of the file
     * @return Set of pairs (file part - peer) to download
     */
    Set<DownloadingDescription> getDownloadingDescription(Set<Integer> localParts, Set<PeerDescription> peerDescriptions);
}
