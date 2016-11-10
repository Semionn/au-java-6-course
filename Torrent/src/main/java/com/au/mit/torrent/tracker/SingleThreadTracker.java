package com.au.mit.torrent.tracker;

import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.CreateNewRequest;
import com.au.mit.torrent.common.protocol.requests.tracker.TrackerRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by semionn on 28.10.16.
 */
public class SingleThreadTracker implements Tracker {
    private final static int DEFAULT_PORT = 8081;
    private final static Logger logger = Logger.getLogger(SingleThreadTracker.class.getName());

    private int filesIdCounter = 0;
    private final Map<Integer, FileDescription> fileDescriptions;
    private final Map<SocketAddress, ClientDescription> clients;
    private Selector selector;
    private final InetSocketAddress listenAddress;

    public SingleThreadTracker(String hostname, int port) {
        listenAddress = new InetSocketAddress(hostname, port);
        clients = new HashMap<>();
        fileDescriptions = new HashMap<>();
    }

    public SingleThreadTracker() {
        this("localhost", DEFAULT_PORT);
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

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();

        ClientDescription client;
        if (clients.containsKey(remoteAddr)) {
            logger.info("Received command from : " + remoteAddr);
            client = clients.get(remoteAddr);
        } else {
            logger.info("Connected to: " + remoteAddr);
            client = new ClientDescription(channel);
            clients.put(remoteAddr, client);
        }

        channel.register(selector, SelectionKey.OP_READ, new CreateNewRequest(client));
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        TrackerRequest request = (TrackerRequest) key.attachment();
        try {
            if (request.handle(channel, this)) {
//                logger.info(String.format("Finished request %s", ((TrackerRequest) key.attachment()).getType().name()));
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

    private void removeClient(ClientDescription client) {
        client.getFileDescriptions().forEach(fd -> fileDescriptions.remove(fd.getId()));
        clients.entrySet().stream().filter(e -> e.getValue().equals(client)).forEach(clients::remove);
    }
}
