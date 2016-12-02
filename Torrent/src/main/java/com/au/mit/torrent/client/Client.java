package com.au.mit.torrent.client;

import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.PeerFileStat;
import com.au.mit.torrent.common.protocol.FileDescription;

public interface Client {
    void connect(String hostname);
    void connect(String hostname, int port);
    void uploadFile(String filePath);
    void listRequest();
    void downloadFile(int fileID);
    PeerFileStat statRequest(FileDescription fileDescription, ClientAddress clientAddress);
    byte[] getRequest(int fileID, int partNum, ClientAddress clientAddress);
    boolean isConnected();
}
