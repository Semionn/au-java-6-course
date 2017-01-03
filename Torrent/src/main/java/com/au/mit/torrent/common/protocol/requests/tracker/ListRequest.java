package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.AsyncWrapper;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.exceptions.AsyncReadRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.AsyncWriteRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.tracker.Tracker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementation of List request for tracker server
 */
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
    public boolean handle(SocketChannel channel, Tracker tracker) throws IOException {
        try {
            async.wrapWrite(() -> fileDescriptions = new ArrayList<>(tracker.getFileDescriptions().values()));
            async.wrapWrite(() -> {
                buffer.putInt(fileDescriptions.size());
                return true;
            });
            async.channelInteract(() -> buffer.writeTo(channel));
            async.forloopWrite(0, fileDescriptions.size(), new AsyncWrapper.IOFunction[]{
                    (i) -> fileDescription = fileDescriptions.get((Integer) i),
                    (i) -> {
                        buffer.putInt(fileDescription.getId());
                        return true;
                    },
                    (i) -> {
                        buffer.putString(fileDescription.getName());
                        return true;
                    },
                    (i) -> {
                        buffer.putLong(fileDescription.getSize());
                        return true;
                    },
                    (i) -> {
                        buffer.writeTo(channel);
                        return true;
                    }
            });
            async.channelInteract(() -> buffer.writeTo(channel));
            return true;
        } catch (AsyncWriteRequestNotCompleteException e) {
            async.setWriteInteractionSupplier(() -> buffer.writeTo(channel));
            return false;
        }
    }

    /**
     * Sends List request to tracker
     * @param channel channel for communication with the tracker
     */
    public static Map<Integer, FileDescription> send(SocketChannel channel) {
        try {
            SmartBuffer bufferWrite = SmartBuffer.allocate(Integer.BYTES);
            bufferWrite.putInt(TrackerRequestType.LIST.getNum());
            bufferWrite.writeSync(channel);

            SmartBuffer bufferRead = SmartBuffer.allocate(1024);
            int filesCount = bufferRead.getIntSync(channel);
            Map<Integer, FileDescription> result = new HashMap<>(filesCount);
            while (result.size() < filesCount) {
                Integer fileID = bufferRead.getIntSync(channel);
                String fileName = bufferRead.getStringSync(channel);
                Long fileSize = bufferRead.getLongSync(channel);
                result.put(fileID, new FileDescription(fileID, fileName, fileSize));
            }
            return result;
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
