package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.AsyncWrapper;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.tracker.Tracker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ListRequest implements TrackerRequest {
    private final static Logger logger = Logger.getLogger(ListRequest.class.getName());

    private ClientDescription client;
    private List<FileDescription> fileDescriptions = null;
    private FileDescription fileDescription = null;
    private SmartBuffer buffer = SmartBuffer.allocate(1024);
    private AsyncWrapper async = new AsyncWrapper();

    public ListRequest(ClientDescription client) {
        this.client = client;
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    @Override
    public TrackerRequestType getType() {
        return TrackerRequestType.LIST;
    }

    @Override
    public boolean handle(SocketChannel channel, Tracker tracker) throws IOException {
        async.wrap(() -> fileDescriptions = new ArrayList<>(tracker.getFileDescriptions().values()));
        async.wrap(() -> {
            buffer.putInt(fileDescriptions.size());
            return true;
        });
        async.channelInteract(() -> buffer.writeTo(channel));
        async.forloop(0, fileDescriptions.size(), new AsyncWrapper.IOFunction[] {
                (i) -> fileDescription = fileDescriptions.get((Integer) i),
                (i) -> { buffer.putInt(fileDescription.getId()); return true; },
                (i) -> { buffer.putString(fileDescription.getName()); return true; },
                (i) -> { buffer.putLong(fileDescription.getSize()); return true; },
                (i) -> { buffer.writeTo(channel); return true; }
        });
        async.channelInteract(() -> buffer.writeTo(channel));
        return true;
    }

    /**
     * Sends List request to tracker
     * @param channel channel for sending port
     */
    public static List<FileDescription> send(SocketChannel channel) {
        try {
            SmartBuffer bufferWrite = SmartBuffer.allocate(Integer.BYTES);
            bufferWrite.putInt(TrackerRequestType.LIST.getNum());
            bufferWrite.writeSync(channel);

            SmartBuffer bufferRead = SmartBuffer.allocate(1024);
            int filesCount = bufferRead.getIntSync(channel);
            List<FileDescription> result = new ArrayList<>(filesCount);
            while (result.size() < filesCount) {
                Integer fileID = bufferRead.getIntSync(channel);
                String fileName = bufferRead.getStringSync(channel);
                Long fileSize = bufferRead.getLongSync(channel);
                result.add(new FileDescription(fileID, fileName, fileSize));
            }
            return result;
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
