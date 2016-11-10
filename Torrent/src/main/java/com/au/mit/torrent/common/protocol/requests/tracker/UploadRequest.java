package com.au.mit.torrent.common.protocol.requests.tracker;

import com.au.mit.torrent.common.AsyncWrapper;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.exceptions.AsyncRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.tracker.Tracker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Class for client and tracker sides handling of new client connection case
 */
public class UploadRequest implements TrackerRequest {
    private final static Logger logger = Logger.getLogger(UploadRequest.class.getName());

    private ClientDescription client;
    private SmartBuffer buffer;
    private AsyncWrapper async;

    private String fileName = null;
    private Long fileSize = null;
    private FileDescription fileDescription = null;
    private Integer fileID = null;
    private SmartBuffer writeBuffer = null;

    public UploadRequest(ClientDescription client) {
        this.client = client;
        buffer = new SmartBuffer(ByteBuffer.allocate(1024));
        async = new AsyncWrapper();
        writeBuffer = new SmartBuffer(ByteBuffer.allocate(Integer.BYTES));
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    @Override
    public TrackerRequestType getType() {
        return TrackerRequestType.UPLOAD;
    }

    @Override
    public boolean handle(SocketChannel channel, Tracker tracker) throws IOException {
        try {
            async.resetCounter();
            async.channelInteract(() -> buffer.readFrom(channel));
            fileName = async.wrap(fileName, () -> buffer.getString());
            fileSize = async.wrap(fileSize, () -> buffer.getLong());
            fileDescription = async.wrap(fileDescription, () -> new FileDescription(fileName, fileSize));
            async.wrap(() -> {
                fileDescription.addSid(client);
                return null;
            });
            fileID = async.wrap(fileID, () -> tracker.addFileDescription(fileDescription));
            async.wrap(() -> {
                writeBuffer.putInt(fileID);
                return null;
            });
            async.channelInteract(() -> writeBuffer.writeTo(channel));
//            async.channelInteract(() -> {
//                channel.shutdownOutput();
//                return 0;
//            });
        } catch (AsyncRequestNotCompleteException e) {
            return false;
        }
        return true;
    }

    /**
     * Sends
     * @param channel channel for sending port
     */
    public static int send(SocketChannel channel, String fileName, Long fileSize) {
        try {
            SmartBuffer bufferWrite = new SmartBuffer(ByteBuffer.allocate(1024));
            bufferWrite.putInt(TrackerRequestType.UPLOAD.getNum());
            bufferWrite.putString(fileName);
            bufferWrite.putLong(fileSize);
            bufferWrite.writeSync(channel);
            SmartBuffer bufferRead = new SmartBuffer(ByteBuffer.allocate(1024));
            return bufferRead.getIntSync(channel);
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
