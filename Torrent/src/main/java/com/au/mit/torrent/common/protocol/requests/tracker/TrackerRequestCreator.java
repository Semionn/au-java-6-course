package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.exceptions.EmptyChannelException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.tracker.Tracker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Class for client and tracker seedes handling of new client connection case
 */
public class TrackerRequestCreator implements TrackerRequest {

    private ClientDescription client;
    private ByteBuffer buffer;

    public TrackerRequestCreator(ClientDescription client) {
        this.client = client;
        this.buffer = ByteBuffer.allocate(Integer.BYTES);
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    @Override
    public boolean handle(SocketChannel channel, Tracker tracker) throws IOException {
        int numRead = channel.read(buffer);
        if (numRead == -1) {
            throw new EmptyChannelException();
        }

        if (buffer.position() == Integer.BYTES) {
            buffer.flip();
            int requestNum = buffer.getInt();
            final TrackerRequestType requestType = TrackerRequestType.getTypeByNum(requestNum);
            TrackerRequest request = null;
            switch (requestType) {
                case LIST:
                    request = new ListRequest(client);
                    break;
                case UPLOAD:
                    request = new UploadRequest(client);
                    break;
                case SOURCES:
                    request = new SourceRequest(client);
                    break;
                case UPDATE:
                    request = new UpdateRequest(client);
                    break;
                case NONE:
                    break;
                case CREATE_REQUEST:
                    break;
            }
            if (request != null) {
                if (!request.handle(channel, tracker)) {
                    tracker.addRequestHandler(channel, request);
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
