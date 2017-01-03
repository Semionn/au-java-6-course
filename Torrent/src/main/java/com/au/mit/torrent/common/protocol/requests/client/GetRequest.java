package com.au.mit.torrent.common.protocol.requests.client;

import com.au.mit.torrent.client.PeerServer;
import com.au.mit.torrent.common.AsyncWrapper;
import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.exceptions.AsyncReadRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.AsyncWriteRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of Get request for peer server
 */
public class GetRequest implements ClientRequest {
    private final static Logger logger = Logger.getLogger(GetRequest.class.getName());

    private ClientDescription client;
    private List<FileDescription> fileDescriptions = null;
    private FileDescription fileDescription = null;
    private Integer fileID = null;
    private Integer partNum = null;
    private PeerFileStat peerFileStat = null;
    private InputStream inputStream = null;
    private SmartBuffer bufferRead = SmartBuffer.allocate(Integer.BYTES * 2);
    private SmartBuffer bufferWrite = SmartBuffer.allocate(PeerFileStat.PART_SIZE);
    private AsyncWrapper async = new AsyncWrapper();

    public GetRequest(ClientDescription client) {
        this.client = client;
    }

    @Override
    public ClientDescription getClient() {
        return client;
    }

    @Override
    public boolean handle(SocketChannel channel, PeerServer peerServer) throws IOException {
        try {
            async.reset();
            async.channelInteract(() -> bufferRead.readFrom(channel));
            async.wrapRead(() -> fileID = bufferRead.getInt());
            async.wrapRead(() -> partNum = bufferRead.getInt());
            async.wrapWrite(() -> {
                peerFileStat = peerServer.getPeerFileStat(fileID);
                if (peerFileStat != null) {
                    inputStream = peerServer.readFilePart(fileID, partNum);
                    byte[] tempBuf = new byte[PeerFileStat.PART_SIZE];
                    bufferWrite.setWriteState();
                    try {
                        inputStream.read(tempBuf);
                        bufferWrite.getBuffer().put(tempBuf);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "File reading failed", e);
                    }
                    return true;
                }
                inputStream = new ByteArrayInputStream(new byte[PeerFileStat.PART_SIZE]);
                return true;
            });

            async.channelInteract(() -> bufferWrite.writeTo(channel));

            async.channelInteract(() -> {
                channel.close();
                return 0;
            });
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
     * Sends Get request to peer server
     * @param channel channel for communication with the peer server
     */
    public static byte[] send(SocketChannel channel, int fileID, int partNum) {
        try {
            SmartBuffer bufferWrite = SmartBuffer.allocate(Integer.BYTES * 3);
            bufferWrite.putInt(ClientRequestType.GET.getNum());
            bufferWrite.putInt(fileID);
            bufferWrite.putInt(partNum);
            bufferWrite.writeSync(channel);

            SmartBuffer bufferRead = SmartBuffer.allocate(PeerFileStat.PART_SIZE);
            bufferRead.readSync(channel);
            return bufferRead.getBuffer().array();
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
