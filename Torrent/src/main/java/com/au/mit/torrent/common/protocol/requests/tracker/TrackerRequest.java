package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.tracker.Tracker;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by semionn on 09.11.16.
 */
public interface TrackerRequest {
    ClientDescription getClient();
    TrackerRequestType getType();

    boolean handle(SocketChannel channel, Tracker tracker) throws IOException;
}
