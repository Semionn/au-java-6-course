package com.au.mit.torrent.client;

import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.client.ClientRequest;
import com.au.mit.torrent.common.protocol.requests.client.ClientRequestCreator;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Server of torrent client, which allows provide access to its own files with requests: Get and Stat
 */
public class PeerServer {
    private final static short DEFAULT_PORT = 8081;
    private final static String DEFAULT_INTERFACE = "0.0.0.0";
    private final static Logger logger = Logger.getLogger(PeerServer.class.getName());

    private Selector selector;
    private final InetSocketAddress listenAddress;

    private final HashMap<Integer, PeerFileStat> filesStats = new HashMap<>();
    private final HashMap<Integer, FileDescription> filesDescriptions = new HashMap<>();

    public PeerServer() {
        this(DEFAULT_PORT);
    }

    public PeerServer(short port) {
        this(DEFAULT_INTERFACE, port);
    }

    public PeerServer(String host, short port) {
        listenAddress = new InetSocketAddress(host, port);
    }

    /**
     * Returns PeerFileStat data object if appropriate file exists, null otherwise
     */
    public PeerFileStat getPeerFileStat(int fileID) {
        if (!filesStats.containsKey(fileID)) {
            return null;
        }
        return filesStats.get(fileID);
    }

    /**
     * Returns input stream to read from specified part of the specified file
     * @param fileID local file id, previously uploaded to the tracker
     * @param partNum part number of the file
     * @return input stream for reading the part
     */
    public InputStream readFilePart(int fileID, int partNum) {
        if (!filesDescriptions.containsKey(fileID)) {
            return null;
        }
        final FileDescription fileDescription = filesDescriptions.get(fileID);
        final int partPosition = PeerFileStat.getPartPosition(partNum);

        if (fileDescription.getSize() <= partPosition) {
            return null;
        }
        try {
            final FileInputStream fileInputStream = new FileInputStream(new File(fileDescription.getLocalPath()));
            final long actualSkipped = fileInputStream.skip(partPosition);
            if (actualSkipped != partPosition) {
                return null;
            }
            return fileInputStream;
        } catch (IOException e) {
            logger.log(Level.WARNING, "File reading failed", e);
            return null;
        }
    }

    /**
     * Starts the peer server
     */
    public void start() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel.socket().bind(listenAddress);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Seed server started...");

            while (!Thread.interrupted()) {
                selector.select();

                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                }
            }
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    /**
     * Adds request handler on reading from channel with additional attachment - ClientRequest.
     * Its handle method will be called after next data receiving from channel
     * @param channel channel for tracking
     * @param request request handler of input data
     * @throws ClosedChannelException
     */
    public void addRequestHandler(SocketChannel channel, ClientRequest request) throws ClosedChannelException {
        channel.register(selector, SelectionKey.OP_READ, request);
    }

    void addLocalFile(FileDescription fileDescription) {
        final int fileID = fileDescription.getId();
        final int partsCount = PeerFileStat.getPartsCount(fileDescription.getSize());
        filesDescriptions.put(fileID, fileDescription);
        filesStats.put(fileID,
                new PeerFileStat(fileID,
                        IntStream.range(0, partsCount).boxed().collect(Collectors.toSet())));
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        logger.info("Connected to peer: " + remoteAddr);

        ClientDescription client = new ClientDescription(channel);
        channel.register(selector, SelectionKey.OP_READ, new ClientRequestCreator(client));
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientRequest request = (ClientRequest) key.attachment();
        try {
            if (request.handle(channel, this)) {
                key.cancel();
            }
        } catch (EmptyChannelException e) {
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            logger.warning("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
        } catch (IOException e) {
            channel.close();
            key.cancel();
        }
    }
}
