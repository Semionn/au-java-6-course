package com.au.mit.torrent.common.protocol;

import com.au.mit.torrent.common.ClientAddress;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FileDescription {
    private int id;
    private String name;
    private long size;
    private String localPath = null;
    private Set<ClientDescription> sids = new HashSet<>();

    public FileDescription(String name, long size) {
        this(-1, name, size, "");
    }

    public FileDescription(int id, String name, long size) {
        this(id, name, size, name);
    }

    public FileDescription(String name, long size, String localPath) {
        this(-1, name, size, localPath);
    }

    public FileDescription(int id, String name, long size, String localPath) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.localPath = localPath;
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

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
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

    public Set<ClientAddress> getSidsAddresses() {
        return sids.stream().map(ClientDescription::getAddress).collect(Collectors.toSet());
    }

    public void setSidsAddresses(Set<ClientAddress> sidsAddresses) {
        sids = sidsAddresses.stream().map(a -> new ClientDescription(null, a)).collect(Collectors.toSet());
    }

    public void addSid(ClientDescription clientDescription) {
        sids.add(clientDescription);
    }

    public void removeClient(ClientDescription clientDescription) {
        sids.remove(clientDescription);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileDescription that = (FileDescription) o;

        if (id != that.id) return false;
        if (size != that.size) return false;
        if (!name.equals(that.name)) return false;
        if (localPath != null ? !localPath.equals(that.localPath) : that.localPath != null) return false;
        return sids != null ? sids.equals(that.sids) : that.sids == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (localPath != null ? localPath.hashCode() : 0);
        result = 31 * result + (sids != null ? sids.hashCode() : 0);
        return result;
    }
}
