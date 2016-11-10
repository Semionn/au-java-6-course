package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.tracker.Tracker;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Class for client and tracker sides handling of new client connection case
 */
public class UpdateRequest implements TrackerRequest {
    private final static Logger logger = Logger.getLogger("UpdateRequest");

    private ClientDescription client;
    private ByteBuffer buffer;

    public UpdateRequest(ClientDescription client) {
        this.client = client;
        buffer = ByteBuffer.allocate(Integer.BYTES);
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    @Override
    public TrackerRequestType getType() {
        return TrackerRequestType.UPDATE;
    }

    @Override
    public boolean handle(SocketChannel channel, Tracker tracker) throws IOException {
        throw new NotImplementedException();
//        int numRead = channel.read(buffer);
//        if (numRead == -1) {
//            throw new IOException();
//        }
//        final IntBuffer intBuffer = buffer.asIntBuffer();
//        if (intBuffer.hasArray() && intBuffer.array().length == 1) {
//            int clientPort = intBuffer.get(0);
//            client.setLocalPort(clientPort);
//        }
//        return true;
    }

    /**
     * Sends local port to tracker with specified SocketChannel
     * @param channel channel for sending port
     * @param localPort local port for receiving incoming queries
     */
    public static void send(SocketChannel channel, int localPort) {
        throw new NotImplementedException();
//        try {
//            ByteBuffer bufferPort = ByteBuffer.allocate(Integer.BYTES);
//            bufferPort.asIntBuffer().put(localPort);
//            channel.write(bufferPort);
//        } catch (IOException e) {
//            throw new CommunicationException(e);
//        }
    }
}
