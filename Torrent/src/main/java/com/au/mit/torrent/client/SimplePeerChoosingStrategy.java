package com.au.mit.torrent.client;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        final List<List<DownloadingDescription>> partsPeers = new ArrayList<>(totalParts.size());
        totalParts.forEach(part -> partsPeers.add(new ArrayList<>()));

        for (PeerDescription peer : peerDescriptions) {
            for (Integer part : peer.getPeerFileStat().getParts()) {
                partsPeers.get(part).add(new DownloadingDescription(peer, part));
            }
        }

        partsPeers.sort((s1, s2) -> s1.size() == s2.size() ? 0 : (s1.size() < s2.size() ? 1 : -1));

        class Counter {
            private int chosenParts = 0;

            private void incParts() {
                chosenParts++;
            }
        }
        final Map<PeerDescription, Counter> priorities = peerDescriptions.stream().collect(Collectors.toMap(Function.identity(), pd -> new Counter()));

        final Set<DownloadingDescription> result = new HashSet<>();
        for (List<DownloadingDescription> partsPeer : partsPeers) {
            DownloadingDescription downlDescr = partsPeer.stream().min((d1, d2) ->
                    Comparator.<Integer>naturalOrder().compare(priorities.get(d1).chosenParts,
                            priorities.get(d2).chosenParts)).get();
            result.add(downlDescr);
            priorities.get(downlDescr.getPeerDescription()).incParts();
        }

        return result;
    }
}
