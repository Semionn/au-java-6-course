package com.au.mit.torrent.common.protocol.requests.client;

import com.au.mit.torrent.client.PeerServer;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;
import com.au.mit.torrent.common.protocol.ClientDescription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Class for client and tracker sides handling of new connection between peers
 */
public class ClientRequestCreator implements ClientRequest {
    private ClientDescription client;
    private ByteBuffer buffer;

    public ClientRequestCreator(ClientDescription client) {
        this.client = client;
        this.buffer = ByteBuffer.allocate(Integer.BYTES);
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    /**
     * Reads int from the channel, creates and starts appropriate request handler (Get or Stat).
     * If command isn't complete after first attempt, it register in peer server for future channel updates
     */
    @Override
    public boolean handle(SocketChannel channel, PeerServer peerServer) throws IOException {
        int numRead = channel.read(buffer);
        if (numRead == -1) {
            throw new EmptyChannelException();
        }

        if (buffer.position() == Integer.BYTES) {
            buffer.flip();
            int requestNum = buffer.getInt();
            final ClientRequestType requestType = ClientRequestType.getTypeByNum(requestNum);
            ClientRequest request = null;
            switch (requestType) {
                case STAT:
                    request = new StatRequest(client);
                    break;
                case GET:
                    request = new GetRequest(client);
                    break;
                case NONE:
                    break;
                case CREATE_REQUEST:
                    break;
            }
            if (request != null) {
                if (!request.handle(channel, peerServer)) {
                    peerServer.addRequestHandler(channel, request);
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
