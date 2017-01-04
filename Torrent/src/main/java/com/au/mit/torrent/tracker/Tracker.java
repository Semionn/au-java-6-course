package com.au.mit.torrent.tracker;

import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.TrackerRequest;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the torrent tracker servers
 */
public interface Tracker {
    /**
     * Starts the server
     */
    void start();

    /**
     * Returns all uploaded to this server file descriptions
     */
    Map<Integer, FileDescription> getFileDescriptions();

    /**
     * Adds provided file description to set of existing files and set new ID for it
     * @param fileDescription file description to add
     * @return ID for the file
     */
    int addFileDescription(FileDescription fileDescription);

    /**
     * Adds handler for the specified channel with the request attachment.
     * Should be called, if reading operation was not completed
     * @param channel to register handler
     * @param request attachment
     * @throws ClosedChannelException
     */
    void addRequestHandler(SocketChannel channel, TrackerRequest request) throws ClosedChannelException;

    /**
     * Updates last access time for the specified client, and checks IDs from the fileIDs set to be existed in the server
     * @param client torrent client
     * @param fileIds IDs of files, previously uploaded by the client
     * @return true, if all IDs already uploaded, false otherwise
     */
    boolean updateSeed(ClientDescription client, Set<Integer> fileIds);
}
