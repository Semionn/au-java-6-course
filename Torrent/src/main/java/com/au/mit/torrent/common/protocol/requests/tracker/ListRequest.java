package com.au.mit.torrent.common.protocol.requests.tracker;

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

/**
 * Class for client and tracker sides handling of new client connection case
 */
public class ListRequest implements TrackerRequest {
    private final static Logger logger = Logger.getLogger("ListRequest");

    private ClientDescription client;

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
        Map<Integer, FileDescription> fileDescriptions = tracker.getFileDescriptions();
        SmartBuffer buffer = new SmartBuffer(ByteBuffer.allocate(1024));
        buffer.putInt(fileDescriptions.size());
        buffer.writeSync(channel);
        for (FileDescription fileDescription : fileDescriptions.values()) {
            buffer.putInt(fileDescription.getId());
            buffer.putString(fileDescription.getName());
            buffer.putLong(fileDescription.getSize());
            buffer.writeSync(channel);
        }
        channel.shutdownOutput();
        return true;
    }

    /**
     * Sends request
     * @param channel channel for sending port
     */
    public static List<FileDescription> send(SocketChannel channel) {
        try {
            ByteBuffer bufferWrite = ByteBuffer.allocate(Integer.BYTES);
            bufferWrite.putInt(TrackerRequestType.LIST.getNum());
            bufferWrite.flip();
            channel.write(bufferWrite);

            SmartBuffer bufferRead = new SmartBuffer(ByteBuffer.allocate(1024));
            int filesCount = bufferRead.getIntSync(channel);
            System.out.println(filesCount);
            List<FileDescription> result = new ArrayList<>(filesCount);
            while (result.size() < filesCount) {
                Integer fileID = bufferRead.getIntSync(channel);
                String fileName = bufferRead.getStringSync(channel);
                Long fileSize = bufferRead.getLongSync(channel);
                System.out.println(fileName);
                result.add(new FileDescription(fileID, fileName, fileSize));
            }
            return result;
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
