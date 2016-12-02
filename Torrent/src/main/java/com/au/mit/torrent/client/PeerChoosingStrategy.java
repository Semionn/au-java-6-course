package com.au.mit.torrent.client;

import java.util.Set;

@FunctionalInterface
public interface PeerChoosingStrategy {
    Set<DownloadingDescription> getDownloadingDescription(Set<Integer> localParts, Set<PeerDescription> peerDescriptions);
}
