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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of Update request for tracker server
 */
public class UpdateRequest implements TrackerRequest {
    private final static Logger logger = Logger.getLogger(UpdateRequest.class.getName());

    private SmartBuffer buffer;
    private AsyncWrapper async;
    private Short clientPort = null;
    private Integer filesCount = null;
    private Set<Integer> fileIds = new HashSet<>();
    private Boolean updated = null;
    private ClientDescription client = null;

    public UpdateRequest() {
        buffer = SmartBuffer.allocate(1024);
        async = new AsyncWrapper();
    }

    @Override
    public boolean handle(SocketChannel channel, Tracker tracker) throws IOException {
        try {
            async.reset();
            async.channelInteract(() -> buffer.readFrom(channel));
            async.wrapRead(() -> clientPort = buffer.getShort());
            async.wrapRead(() -> {
                client = new ClientDescription(channel);
                client.setLocalPort(clientPort);
                return true;
            });
            async.wrapRead(() -> filesCount = buffer.getInt());
            async.forloopRead(0, filesCount, new AsyncWrapper.IOFunction[] {
                    (i) -> {
                        final Integer id = buffer.getInt();
                        if (id == null) {
                            return null;
                        }
                        fileIds.add(id);
                        return true;
                    },
                    (i) -> buffer.readFrom(channel)
            });
            async.wrapRead(() -> updated = tracker.updateSeed(client, fileIds));
            async.wrapWrite(() -> {
                buffer.putBool(updated);
                return true;
            });
            async.channelInteract(() -> buffer.writeTo(channel));
        } catch (AsyncReadRequestNotCompleteException e) {
            async.setReadInteractionSupplier(() -> buffer.readFrom(channel));
            return false;
        } catch (AsyncWriteRequestNotCompleteException e) {
            async.setWriteInteractionSupplier(() -> buffer.writeTo(channel));
            return false;
        }
        return true;
    }

    /**
     * Sends Update request to tracker
     * @param channel channel for communication with the tracker
     */
    public static boolean send(SocketChannel channel, short localPort, Set<FileDescription> localFiles) {
        try {
            SmartBuffer buffer = new SmartBuffer(ByteBuffer.allocate(Integer.BYTES*4));
            buffer.putInt(TrackerRequestType.UPDATE.getNum());
            buffer.putShort(localPort);
            buffer.putInt(localFiles.size());
            for (FileDescription file : localFiles) {
                buffer.putInt(file.getId());
                buffer.writeSync(channel);
            }
            buffer.writeSync(channel);
            return buffer.getBoolSync(channel);
        } catch (IOException e) {
            throw new CommunicationException(e);
        }

    }
}
