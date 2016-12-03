package com.au.mit.torrent.common.protocol.requests.client;

import com.au.mit.torrent.client.PeerServer;
import com.au.mit.torrent.common.protocol.ClientDescription;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Interface for handling reading operation from specified channel during the peer-to-peer connection
 */
public interface ClientRequest {
    /**
     * Returns description of client, which initiates the connection
     */
    ClientDescription getClient();

    /**
     * Handler for channel reading operation
     * @param channel channel to another peer (torrent client)
     * @param peerServer the local peer server
     * @return true, if handling has done successfully, false otherwise
     * @throws IOException
     */
    boolean handle(SocketChannel channel, PeerServer peerServer) throws IOException;
}
