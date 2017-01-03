package com.au.mit.torrent.tracker;

import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.TrackerRequest;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;

public interface Tracker {
    void start();
    Map<Integer, FileDescription> getFileDescriptions();
    int addFileDescription(FileDescription fileDescription);
    void addRequestHandler(SocketChannel channel, TrackerRequest request) throws ClosedChannelException;
    boolean updateSeed(ClientDescription client, Set<Integer> fileIds);
}
