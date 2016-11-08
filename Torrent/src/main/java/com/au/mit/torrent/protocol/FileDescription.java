package com.au.mit.torrent.protocol;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by semionn on 28.10.16.
 */
public class FileDescription {
    private String fileName;
    private Set<ClientDescription> sids;

    public FileDescription(String fileName) {
        this.fileName = fileName;
        sids = new HashSet<>();
    }

    public String getFileName() {
        return fileName;
    }

    public Set<ClientDescription> getSids() {
        return sids;
    }

    public void addClient(ClientDescription clientDescription) {
        sids.add(clientDescription);
    }

    public void removeClient(ClientDescription clientDescription) {
        sids.remove(clientDescription);
    }


}
