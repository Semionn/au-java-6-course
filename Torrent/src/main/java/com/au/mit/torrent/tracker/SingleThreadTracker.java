package com.au.mit.torrent.tracker;

import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.TrackerRequestCreator;
import com.au.mit.torrent.common.protocol.requests.tracker.TrackerRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Torrent tracker server implementation with nonblocking nio
 */
public class SingleThreadTracker implements Tracker {
    private final static short DEFAULT_PORT = 8081;
    private final static String DEFAULT_INTERFACE = "0.0.0.0";
    private final static Logger logger = Logger.getLogger(SingleThreadTracker.class.getName());
    private final static int UPDATE_TIME_MIN = 5;

    private int filesIdCounter = 0;
    private final Map<Integer, FileDescription> fileDescriptions;
    private final Map<ClientAddress, ClientDescription> clients;
    private Selector selector;
    private final InetSocketAddress listenAddress;

    public SingleThreadTracker(String hostname, int port) {
        listenAddress = new InetSocketAddress(hostname, port);
        clients = new HashMap<>();
        fileDescriptions = new HashMap<>();
    }

    public SingleThreadTracker() {
        this(DEFAULT_INTERFACE, DEFAULT_PORT);
    }

    @Override
    public Map<Integer, FileDescription> getFileDescriptions() {
        return fileDescriptions;
    }

    @Override
    public int addFileDescription(FileDescription fileDescription) {
        int id = filesIdCounter;
        fileDescription.setId(id);
        fileDescriptions.put(id, fileDescription);
        filesIdCounter++;
        return id;
    }

    @Override
    public void start() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel.socket().bind(listenAddress);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Server started...");

            while (!Thread.interrupted()) {
                selector.select();
                checkClients();

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

    @Override
    public void addRequestHandler(SocketChannel channel, TrackerRequest request) throws ClosedChannelException {
        channel.register(selector, SelectionKey.OP_READ, request);
    }

    @Override
    public boolean updateSeed(ClientDescription client, Set<Integer> fileIds) {
        client.updateAccessTime();
        clients.put(client.getAddress(), client);
        if (fileIds.stream().anyMatch(fileId -> !fileDescriptions.containsKey(fileId))) {
            return false;
        }
        client.clearFileDescriptions();
        for (Integer fileId : fileIds) {
            client.addFile(fileDescriptions.get(fileId));
        }
        return true;
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        logger.info("Connected to peer: " + remoteAddr);

        ClientDescription client = new ClientDescription(channel);
        channel.register(selector, SelectionKey.OP_READ, new TrackerRequestCreator(client));
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        TrackerRequest request = (TrackerRequest) key.attachment();
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

    private void checkClients() {
        final List<ClientDescription> oldClients = clients.entrySet().stream()
                .filter(entry ->
                        entry.getValue().getLastAccessTime().plusMinutes(UPDATE_TIME_MIN).isBefore(LocalDateTime.now()))
                .map(Map.Entry::getValue).collect(Collectors.toList());
        oldClients.forEach(client -> clients.remove(client.getAddress()));
        oldClients.forEach(client -> client.getFileDescriptions().forEach(fd -> fd.removeClient(client)));
    }
}
