package com.au.mit.torrent.common.protocol.requests.client;

import com.au.mit.torrent.client.PeerServer;
import com.au.mit.torrent.common.protocol.ClientDescription;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface ClientRequest {
    ClientDescription getClient();
    ClientRequestType getType();

    boolean handle(SocketChannel channel, PeerServer peerServer) throws IOException;
}
