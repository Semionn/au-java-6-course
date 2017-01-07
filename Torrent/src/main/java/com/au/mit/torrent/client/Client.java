package com.au.mit.torrent.client;

import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.protocol.FileDescription;

/**
 * Interface for torrent client.
 * Allows to perform requests: list, stat, upload, get and mixed query - download file.
 */
public interface Client {
    /**
     * Connects client to tracker with specified hostname and default port
     * Performs update and list queries
     * @param hostname tracker hostname
     */
    void connect(String hostname);

    /**
     * Connects client to tracker with specified hostname and default port
     * Performs update and list queries
     * @param hostname tracker hostname
     * @param port tracker port
     */
    void connect(String hostname, int port);

    /**
     * Uploads file with specified path to tracker
     * @param filePath path to file to upload
     */
    void uploadFile(String filePath);

    /**
     * Performs List request to connected tracker
     */
    void listRequest();

    /**
     * Downloads file from peers retrieved from the tracker
     * @param fileID file id at the tracker
     */
    boolean downloadFile(int fileID);

    /**
     * Performs Stat request to specified peer
     * @param fileDescription target file to get stat info
     * @param clientAddress address of torrent peer
     * @return description of existing parts in the peer
     */
    PeerFileStat statRequest(FileDescription fileDescription, ClientAddress clientAddress);

    /**
     * Performs Get request to specified peer
     * @param fileID id of file to get it's part
     * @param partNum part number of the file
     * @param clientAddress address of peer
     * @return file part as byte array
     */
    byte[] getRequest(int fileID, int partNum, ClientAddress clientAddress);

    /**
     * Returns true if the client is connected to the tracker, false otherwise
     */
    boolean isConnected();
}
