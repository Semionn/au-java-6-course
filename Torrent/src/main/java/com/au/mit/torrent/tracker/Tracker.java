package com.au.mit.torrent.tracker;

import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.TrackerRequest;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * Created by Semionn on 08.11.2016.
 */
public interface Tracker {
    void start();
    Map<Integer, FileDescription> getFileDescriptions();
    int addFileDescription(FileDescription fileDescription);
    void addRequestHandler(SocketChannel channel, TrackerRequest request) throws ClosedChannelException;
}
