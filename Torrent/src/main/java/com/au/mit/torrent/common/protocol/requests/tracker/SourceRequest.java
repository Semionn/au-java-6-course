package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.AsyncWrapper;
import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.exceptions.AsyncRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.tracker.Tracker;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

public class SourceRequest implements TrackerRequest {
    private final static Logger logger = Logger.getLogger(SourceRequest.class.getName());

    private ClientDescription client;
    private SmartBuffer buffer;
    private AsyncWrapper async;

    private ClientAddress clientAddress = null;
    private Integer fileID = null;
    private Integer sidsCount = null;
    private FileDescription fileDescription = null;
    private List<ClientDescription> sids = null;
    private SmartBuffer writeBuffer = null;

    public SourceRequest(ClientDescription client) {
        this.client = client;
        buffer = SmartBuffer.allocate(1024);
        async = new AsyncWrapper();
        writeBuffer = new SmartBuffer(ByteBuffer.allocate(Integer.BYTES));
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    @Override
    public TrackerRequestType getType() {
        return TrackerRequestType.SOURCES;
    }

    @Override
    public boolean handle(SocketChannel channel, Tracker tracker) throws IOException {
        try {
            async.resetCounter();
            async.channelInteract(() -> buffer.readFrom(channel));
            async.wrap(() -> fileID = buffer.getInt());
            async.wrap(() -> fileDescription = tracker.getFileDescriptions().get(fileID));
            async.wrap(() -> sids = new ArrayList<>(fileDescription.getSids()));
            async.wrap(() -> sidsCount = sids.size());
            async.wrap(() -> { buffer.putInt(sidsCount); return true; });
            async.channelInteract(() -> writeBuffer.writeTo(channel));
            async.forloop(0, sidsCount, new AsyncWrapper.IOFunction[] {
                    (i) -> clientAddress = sids.get((Integer) i).getAddress(),
                    (i) -> { buffer.putByte(clientAddress.getIPByte((byte) 0)); return true; },
                    (i) -> { buffer.putByte(clientAddress.getIPByte((byte) 1)); return true; },
                    (i) -> { buffer.putByte(clientAddress.getIPByte((byte) 2)); return true; },
                    (i) -> { buffer.putByte(clientAddress.getIPByte((byte) 3)); return true; },
                    (i) -> { buffer.putShort(clientAddress.getPort()); return true; },
                    (i) -> buffer.writeTo(channel)
            });
            async.channelInteract(() -> writeBuffer.writeTo(channel));
        } catch (AsyncRequestNotCompleteException e) {
            return false;
        }
        return true;
    }

    public static Set<ClientAddress> send(SocketChannel channel, int fileID) {
        try {
            SmartBuffer bufferWrite = SmartBuffer.allocate(1024);
            bufferWrite.putInt(TrackerRequestType.SOURCES.getNum());
            bufferWrite.putInt(fileID);
            bufferWrite.writeSync(channel);
            SmartBuffer bufferRead = SmartBuffer.allocate(1024);
            int sidsCount = bufferRead.getIntSync(channel);
            Set<ClientAddress> sids = new HashSet<>();
            byte[] ip = new byte[4];
            short clientPort;
            String clientIP;
            for (int i = 0; i < sidsCount; i++) {
                ip[0] = bufferRead.getByteSync(channel);
                ip[1] = bufferRead.getByteSync(channel);
                ip[2] = bufferRead.getByteSync(channel);
                ip[3] = bufferRead.getByteSync(channel);
                clientPort = bufferRead.getShortSync(channel);
                clientIP = String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
                sids.add(new ClientAddress(clientIP, clientPort));
            }
            return sids;
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
