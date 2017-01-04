package com.au.mit.torrent.client;

import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.exceptions.DisconnectException;
import com.au.mit.torrent.common.exceptions.RequestException;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.client.GetRequest;
import com.au.mit.torrent.common.protocol.requests.client.StatRequest;
import com.au.mit.torrent.common.protocol.requests.tracker.ListRequest;
import com.au.mit.torrent.common.protocol.requests.tracker.SourceRequest;
import com.au.mit.torrent.common.protocol.requests.tracker.UpdateRequest;
import com.au.mit.torrent.common.protocol.requests.tracker.UploadRequest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
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
    private final short localPort;
    private final PeerServer peerServer;
    private Map<Integer, FileDescription> trackerFiles = new HashMap<>();
    private SimplePeerChoosingStrategy peerChoosingStrategy = new SimplePeerChoosingStrategy(); //DI for losers
    private String trackerHostname = null;
    private Integer trackerPort = null;
    private Thread updateThread = null;

    public ClientImpl(short localPort) {
        this(localPort, new HashSet<>());
    }

    public ClientImpl(short localPort, Set<Path> filesPaths) {
        this.localPort = localPort;
        localFiles = filesPaths.stream()
                .map(path -> new FileDescription(path.getFileName().toString(), new File(path.toString()).length()))
                .collect(Collectors.toSet());
        peerServer = new PeerServer(localPort);
        new Thread(peerServer::start).start();
    }

    @Override
    public void connect(String hostname) {
        connect(hostname, 8081);
    }

    @Override
    public void connect(String hostname, int port) {
        if (updateThread != null) {
            updateThread.interrupt();
            updateThread = null;
        }
        trackerHostname = hostname;
        trackerPort = port;
        updateRequest();
        listRequest();
        updateThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(5 * 60 * 1000);
                    updateRequest();
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    @Override
    public void uploadFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RequestException(String.format("File '%s' not found", filePath));
        }

        final FileDescription fileDescription = new FileDescription(file.getName(), file.length(), filePath);
        uploadFileRequest(fileDescription);
        peerServer.addLocalFile(fileDescription);
        updateRequest();
    }

    @Override
    public void listRequest() {
        checkConnection();
        trackerFiles = sendRequest(trackerHostname, trackerPort, ListRequest::send);
        logger.info(getTrackerFilesDescription());
    }

    @Override
    public void downloadFile(int fileID) {
        listRequest();
        if (trackerFiles.containsKey(fileID)) {
            final FileDescription fileDescription = trackerFiles.get(fileID);
            final Set<ClientAddress> seeds = fileDescription.getSeedsAddresses();

            final HashSet<PeerDescription> peerDescriptions = new HashSet<>();
            for (ClientAddress peerAddress : seeds) {
                final PeerFileStat peerFileStat = statRequest(fileDescription, peerAddress);
                peerDescriptions.add(new PeerDescription(peerAddress, peerFileStat));
            }

            final Set<DownloadingDescription> downloadingDescriptions =
                    peerChoosingStrategy.getDownloadingDescription(new HashSet<>(), peerDescriptions);

            for (DownloadingDescription downloadingDescr : downloadingDescriptions) {
                try {
                    final RandomAccessFile file = new RandomAccessFile(new File(fileDescription.getLocalPath()), "rw");
                    Integer partNum = downloadingDescr.getPartNum();
                    final byte[] part = getRequest(fileID, partNum, downloadingDescr.getPeerDescription().getPeerAddress());
                    file.seek(PeerFileStat.getPartPosition(partNum));
                    file.write(part, 0, PeerFileStat.calcPartSize(partNum, fileDescription.getSize()));
                    file.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "File writing failed", e);
                }
            }
            logger.info(String.format("File '%s' successfully downloaded", fileDescription.getName()));
        } else {
            logger.warning(String.format("File with id '%s' not found", fileID));
        }
    }

    @Override
    public PeerFileStat statRequest(FileDescription fileDescription, ClientAddress clientAddress) {
        checkConnection();
        return sendRequest(clientAddress.getHostIP(), clientAddress.getPort(), (channel) -> StatRequest.send(channel, fileDescription.getId()));
    }

    @Override
    public byte[] getRequest(int fileID, int partNum, ClientAddress clientAddress) {
        checkConnection();
        return sendRequest(clientAddress.getHostIP(), clientAddress.getPort(), (channel) -> GetRequest.send(channel, fileID, partNum));
    }

    @Override
    public boolean isConnected() {
        return trackerHostname != null && trackerPort != null;
    }

    private void checkConnection() {
        if (!isConnected()) {
            throw new DisconnectException("Client is not connected to tracker");
        }
    }

    private void updateRequest() {
        sendRequest(trackerHostname, trackerPort, (channel) -> {
            if (!UpdateRequest.send(channel, localPort, localFiles)) {
                throw new RequestException("Update request failed");
            }
            return true;
        });
    }

    private void uploadFileRequest(FileDescription fileDescription) {
        checkConnection();
        final String fileName = fileDescription.getName();
        final Long fileSize = fileDescription.getSize();
        Integer id = sendRequest(trackerHostname, trackerPort, channel -> UploadRequest.send(channel, fileName, fileSize));
        logger.info(String.format("File %s uploaded with id: %d", fileName, id));
        fileDescription.setId(id);
        localFiles.add(fileDescription);
    }

    private <R> R sendRequest(String hostname, int port, Function<SocketChannel, R> request) {
        try {
            InetSocketAddress hostAddress = new InetSocketAddress(hostname, port);
            SocketChannel socketChannel = null;
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    socketChannel = SocketChannel.open(hostAddress);
                    break;
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

    private String getTrackerFilesDescription() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("Tracker files count: %d%n", trackerFiles.size()));
        stringBuilder.append(String.format("ID; Name; Size; Seeds%n"));
        for (FileDescription file : trackerFiles.values()) {
            stringBuilder.append(String.format("%d; %s; %d; %d%n", file.getId(),
                    file.getName(), file.getSize(), file.getSeeds().size()));
            Set<ClientAddress> seeds = sendRequest(trackerHostname, trackerPort,
                    (channel) -> SourceRequest.send(channel, file.getId()));
            file.setSeedsAddresses(seeds);
            if (seeds != null) {
                for (ClientAddress seed : seeds) {
                    stringBuilder.append(String.format("\tSeed: %s:%s%n", seed.getHostIP(), seed.getPort()));
                }
            }
        }
        return stringBuilder.toString();
    }
}
