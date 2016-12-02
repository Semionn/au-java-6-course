package com.au.mit.torrent.common.protocol.requests.client;

import com.au.mit.torrent.client.PeerServer;
import com.au.mit.torrent.common.AsyncWrapper;
import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.exceptions.AsyncReadRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.AsyncWriteRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.ClientDescription;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public class StatRequest implements ClientRequest {
    private final static Logger logger = Logger.getLogger(StatRequest.class.getName());

    private ClientDescription client;
    private Integer fileID = null;
    private PeerFileStat peerFileStat = null;
    private List<Integer> parts = null;
    private final SmartBuffer bufferWrite = SmartBuffer.allocate(1024);
    private final SmartBuffer bufferRead = SmartBuffer.allocate(Integer.BYTES);
    private final AsyncWrapper async = new AsyncWrapper();

    public StatRequest(ClientDescription client) {
        this.client = client;
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    @Override
    public ClientRequestType getType() {
        return ClientRequestType.STAT;
    }

    @Override
    public boolean handle(SocketChannel channel, PeerServer peerServer) throws IOException {
        try {
            async.resetCounter();
            async.channelInteract(() -> bufferRead.readFrom(channel));
            async.wrapRead(() -> fileID = bufferRead.getInt());
            async.wrapRead(() -> {
                peerFileStat = peerServer.getPeerFileStat(fileID);
                if (peerFileStat != null) {
                    parts = new ArrayList<>(peerFileStat.getParts());
                    bufferWrite.putInt(parts.size());
                    return true;
                }
                parts = new ArrayList<>();
                bufferWrite.putInt(0);
                return true;
            });
            async.forloopWrite(0, parts.size(), new AsyncWrapper.IOFunction[] {
                    (i) -> {
                        bufferWrite.putInt(parts.get((Integer) i));
                        return bufferWrite.writeTo(channel);
                    }
            });
            async.channelInteract(() -> bufferWrite.writeTo(channel));
        } catch (AsyncReadRequestNotCompleteException e) {
            async.channelInteract(() -> bufferRead.readFrom(channel));
            return false;
        } catch (AsyncWriteRequestNotCompleteException e) {
            async.channelInteract(() -> bufferWrite.writeTo(channel));
            return false;
        }
        return true;
    }

    /**
     * Sends Stat request to peer server
     * @param channel channel for sending port
     */
    public static PeerFileStat send(SocketChannel channel, int fileID) {
        try {
            SmartBuffer bufferWrite = SmartBuffer.allocate(Integer.BYTES * 2);
            bufferWrite.putInt(ClientRequestType.STAT.getNum());
            bufferWrite.putInt(fileID);
            bufferWrite.writeSync(channel);

            SmartBuffer bufferRead = SmartBuffer.allocate(1024);
            int partsCount = bufferRead.getIntSync(channel);
            final HashSet<Integer> parts = new HashSet<>();
            for (int i = 0; i < partsCount; i++) {
                parts.add(bufferRead.getIntSync(channel));
            }
            return new PeerFileStat(fileID, parts);
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
