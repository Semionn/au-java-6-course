package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.tracker.Tracker;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Interface for handling reading operation from specified channel during the peer-to-tracker connection
 */
public interface TrackerRequest {
    /**
     * Returns description of client, which initiates the connection
     */
    ClientDescription getClient();

    /**
     * Handler for channel reading operation
     * @param channel channel to peer (torrent client)
     * @param tracker the local tracker server
     * @return true, if handling has done successfully, false otherwise
     * @throws IOException
     */
    boolean handle(SocketChannel channel, Tracker tracker) throws IOException;
}
