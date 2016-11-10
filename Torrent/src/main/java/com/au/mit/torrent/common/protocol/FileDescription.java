package com.au.mit.torrent.common.protocol;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by semionn on 28.10.16.
 */
public class FileDescription {
    private int id;
    private String name;
    private long size;
    private Set<ClientDescription> sids = new HashSet<>();

    public FileDescription(String name, long size) {
        this(-1, name, size);
    }

    public FileDescription(int id, String name, long size) {
        this.id = id;
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public Set<ClientDescription> getSids() {
        return sids;
    }

    public void setSids(Set<ClientDescription> sids) {
        this.sids = sids;
    }

    public void addSid(ClientDescription clientDescription) {
        sids.add(clientDescription);
    }

    public void removeClient(ClientDescription clientDescription) {
        sids.remove(clientDescription);
    }


}
