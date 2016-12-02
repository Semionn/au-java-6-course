package com.au.mit.torrent.client;

import java.util.*;

/**
 * Implementation of PeerChoosingStrategy interface.
 * Greedy algorithm: first, get parts from peers with lower number of available parts and the rest peers next
 */
public class SimplePeerChoosingStrategy implements PeerChoosingStrategy {
    @Override
    public Set<DownloadingDescription> getDownloadingDescription(Set<Integer> localParts, Set<PeerDescription> peerDescriptions) {
        Set<Integer> totalParts = new HashSet<>();
        peerDescriptions.forEach(peer -> totalParts.addAll(peer.getPeerFileStat().getParts()));
        totalParts.removeAll(localParts);

        final List<Set<PeerDescription>> partsPeers = new ArrayList<>(totalParts.size());
        totalParts.forEach(part -> partsPeers.add(new HashSet<>()));

        for (PeerDescription peer : peerDescriptions) {
            for (Integer part : peer.getPeerFileStat().getParts()) {
                partsPeers.get(part).add(peer);
            }
        }

        partsPeers.sort((s1, s2) -> s1.size() == s2.size() ? 0 : (s1.size() < s2.size() ? 1 : -1));

        final Set<DownloadingDescription> result = new HashSet<>();

        for (int i = 0; i < partsPeers.size(); i++) {
            final Iterator<PeerDescription> peerIterator = partsPeers.get(i).iterator();
            if (!peerIterator.hasNext()) {
                continue;
            }
            PeerDescription firstPeer = peerIterator.next();
            result.add(new DownloadingDescription(firstPeer, i));
            for (int j = i; j < partsPeers.size(); j++) {
                partsPeers.get(i).remove(firstPeer);
            }
        }

        return result;
    }
}
