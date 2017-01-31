package com.au.mit.torrent.client;

import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.exceptions.ClientMetadataSerializationException;
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

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Implementation of torrent tracker client
 */
public class ClientImpl implements Client {
    private final static int CONNECTION_RETRIES = 3;
    private final static int RETRY_TIME_MS = 1000;
    private final static Logger logger = Logger.getLogger(ClientImpl.class.getName());

    private final short localPort;
    private final PeerServer peerServer;
    private final DownloadingListener downloadingListener;
    private final Path torrentFolder;
    private Set<TorrentFile> localFiles = new HashSet<>();
    private Map<Integer, FileDescription> trackerFiles = new HashMap<>();
    private SimplePeerChoosingStrategy peerChoosingStrategy = new SimplePeerChoosingStrategy(); //DI for losers
    private String trackerHostname = null;
    private Integer trackerPort = null;
    private Thread updateThread = null;
    private boolean isConnected = false;

    public ClientImpl(short localPort, Path torrentsFolder) {
        this(localPort, torrentsFolder, null);
    }

    public ClientImpl(short localPort, Path torrentsFolder, DownloadingListener downloadingListener) {
        loadMetadata(torrentsFolder);
        this.localPort = localPort;
        this.torrentFolder = torrentsFolder;
        this.downloadingListener = downloadingListener;
        peerServer = new PeerServer(localPort);
        final Thread peerServerThread = new Thread(peerServer::start);
        peerServerThread.setDaemon(true);
        peerServerThread.start();
    }

    public Set<TorrentFile> getLocalFiles() {
        return localFiles;
    }

    public Map<Integer, FileDescription> getTrackerFiles() {
        return trackerFiles;
    }

    @Override
    public void connect(String hostname) {
        connect(hostname, 8081);
    }

    @Override
    public void connect(String hostname, int port) {
        isConnected = false;
        if (updateThread != null) {
            updateThread.interrupt();
            updateThread = null;
        }
        trackerHostname = hostname;
        trackerPort = port;
        updateRequest();
        isConnected = true;
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
    public boolean downloadFile(int fileID) {
        if (localFiles.stream().anyMatch(f -> f.getFileDescription().getId() == fileID)) {
            return false;
        }
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

            final TorrentFile torrentFile = new TorrentFile(fileDescription, 0);
            localFiles.add(torrentFile);

            new File(torrentFolder.toAbsolutePath().toString()).mkdirs();
            new Thread(() -> {
                for (DownloadingDescription downloadingDescr : downloadingDescriptions) {
                    try {
                        final Path filePath = torrentFolder.resolve(fileDescription.getLocalPath());
                        final RandomAccessFile file = new RandomAccessFile(new File(filePath.toAbsolutePath().toString()), "rw");
                        Integer partNum = downloadingDescr.getPartNum();
                        final byte[] part = getRequest(fileID, partNum, downloadingDescr.getPeerDescription().getPeerAddress());
                        file.seek(PeerFileStat.getPartPosition(partNum));
                        file.write(part, 0, PeerFileStat.calcPartSize(partNum, fileDescription.getSize()));
                        file.close();
                        torrentFile.addPart();
                        if (torrentFile.getRatio() == 1.0) {
                            logger.info(String.format("File '%s' successfully downloaded", fileDescription.getName()));
                        }
                        if (downloadingListener != null) {
                            downloadingListener.update(torrentFile);
                        }
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "File writing failed", e);
                    }
                }
            }).start();
            return true;
        } else {
            final String message = String.format("File with id '%s' not found", fileID);
            logger.warning(message);
            throw new RequestException(message);
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
        return isConnected;
    }

    @Override
    public void storeMetadata(Path path) {
        try {
            new File(path.toString()).mkdir();
            FileOutputStream fileOut =
                    new FileOutputStream(getMetaInfoPath(path).toString());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(localFiles);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            throw new ClientMetadataSerializationException(e);
        }
    }

    @Override
    public void loadMetadata(Path path) {
        if (!Files.exists(getMetaInfoPath(path))) {
            logger.log(Level.WARNING, String.format("Metadata file wasn't found: %s", getMetaInfoPath(path)));
            return;
        }
        try {
            FileInputStream fileIn = new FileInputStream(getMetaInfoPath(path).toString());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            localFiles = (Set<TorrentFile>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Metadata loading failed, use default instead", e);
            localFiles = new HashSet<>();
        }
    }

    private static Path getMetaInfoPath(Path storagePath) {
        return storagePath.resolve("meta");
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
        localFiles.add(new TorrentFile(fileDescription));
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
