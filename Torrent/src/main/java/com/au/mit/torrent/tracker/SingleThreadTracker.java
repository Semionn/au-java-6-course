package com.au.mit.torrent.tracker;

import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.protocol.ClientDescription;
import com.au.mit.torrent.protocol.FileDescription;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by semionn on 28.10.16.
 */
public class SingleThreadTracker implements Tracker {
    private static final int DEFAULT_PORT = 8099;
    private final Logger logger = Logger.getLogger("SingleThreadTracker");

    private Map<String, FileDescription> fileDescriptions;
    private Set<ClientDescription> clients;
    private Selector selector;
    private final Map<SocketChannel, List> dataMapper = new HashMap<>();
    private final InetSocketAddress listenAddress;

    public SingleThreadTracker(String hostname, int port) {
        listenAddress = new InetSocketAddress(hostname, port);
    }

    public SingleThreadTracker() {
        this("localhost", DEFAULT_PORT);
    }

    @Override
    public void start() {
        try {
            this.selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            // retrieve server socket and bind to port
            serverChannel.socket().bind(listenAddress);
            serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

            logger.info("Server started...");

            while (true) {
                // wait for events
                this.selector.select();

                //work on selected keys
                Iterator keys = this.selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    // this is necessary to prevent the same key from coming up
                    // again the next time around.
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        this.accept(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    }
                }
            }
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    @Override
    public void stop() {

    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        logger.info("Connected to: " + remoteAddr);

        // register channel with selector for further IO
        dataMapper.put(channel, new ArrayList());
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    //read from the socket channel
    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = channel.read(buffer);

        if (numRead == -1) {
            this.dataMapper.remove(channel);
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            logger.info("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        logger.info("Got: " + new String(data));
    }

    private void removeClient(ClientDescription clientDescription) {
        clientDescription.getFileDescriptions().forEach(fd -> fileDescriptions.remove(fd.getFileName()));
        clients.remove(clientDescription);
    }
}
