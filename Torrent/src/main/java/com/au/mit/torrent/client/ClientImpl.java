package com.au.mit.torrent.client;

import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.exceptions.DisconnectException;
import com.au.mit.torrent.common.exceptions.RequestException;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.ListRequest;
import com.au.mit.torrent.common.protocol.requests.tracker.UploadRequest;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Implementation of torrent tracker client
 */
public class ClientImpl implements Client {
    private final static int CONNECTION_RETRIES = 3;
    private final static int RETRY_TIME_MS = 1000;
    private final static Logger logger = Logger.getLogger(ClientImpl.class.getName());

    private final Set<FileDescription> localFiles;
    private final int localPort;
    private List<FileDescription> trackerFiles = new ArrayList<>();
    private String trackerHostname = null;
    private Integer trackerPort = null;

    public ClientImpl(int localPort) {
        this(localPort, new HashSet<>());
    }

    public ClientImpl(int localPort, Set<Path> filesPaths) {
        this.localPort = localPort;
        localFiles = filesPaths.stream()
                .map(path -> new FileDescription(path.getFileName().toString(), new File(path.toString()).length()))
                .collect(Collectors.toSet());
    }

    public void connect(String hostname) {
        connect(hostname, 8081);
    }

    public void connect(String hostname, int port) {
        trackerHostname = hostname;
        trackerPort = port;
        updateTrackerFiles();
    }

    private <R> R sendRequest(String hostname, int port, Function<SocketChannel, R> request) {
        try {
            InetSocketAddress hostAddress = new InetSocketAddress(hostname, port);
            SocketChannel socketChannel = null;
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    socketChannel = SocketChannel.open(hostAddress);
                } catch (IOException e) {
                    logger.warning(String.format("Attempt %d: Connection opening failed due to error: %s", i, e.getMessage()));
                    Thread.sleep(RETRY_TIME_MS);
                }
            }
            if (socketChannel == null) {
                throw new CommunicationException(String.format("Connection refused after %d attempts", CONNECTION_RETRIES));
            }
            R res = request.apply(socketChannel);
            socketChannel.close();
            return res;
        } catch (IOException e) {
            throw new CommunicationException(e);
        } catch (InterruptedException ignored) { }
        return null;
    }

    public void uploadFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RequestException(String.format("File '%s' not found", filePath));
        }
        uploadFile(new FileDescription(file.getName(), file.length()));
    }

    private void uploadFile(FileDescription fileDescription) {
        checkConnection();
        final String fileName = fileDescription.getName();
        final Long fileSize = fileDescription.getSize();
        Integer id = sendRequest(trackerHostname, trackerPort, channel -> UploadRequest.send(channel, fileName, fileSize));
        logger.info(String.format("File %s uploaded with id: %d", fileName, id));
    }

    public void updateTrackerFiles() {
        checkConnection();
        trackerFiles = sendRequest(trackerHostname, trackerPort, ListRequest::send);
        logger.info(String.format("Tracker files count: %d", trackerFiles.size()));
        logger.info("ID; Name; Size; Seeds");
        for (FileDescription file : trackerFiles) {
            logger.info(String.format("%d; %s; %d; %d", file.getId(), file.getName(), file.getSize(), file.getSids().size()));
        }
    }

    public List<FileDescription> getTrackerFiles() {
        return trackerFiles;
    }

    public void checkConnection() {
        if (!isConnected()) {
            throw new DisconnectException("Client is not connected to tracker");
        }
    }

    public boolean isConnected() {
        return trackerHostname != null && trackerPort != null;
    }
}
