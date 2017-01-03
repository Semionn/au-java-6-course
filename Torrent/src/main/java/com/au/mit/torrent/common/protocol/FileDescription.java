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
    private Set<ClientDescription> seeds = new HashSet<>();

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

    public Set<ClientDescription> getSeeds() {
        return seeds;
    }

    public void setSeeds(Set<ClientDescription> seeds) {
        this.seeds = seeds;
    }

    public Set<ClientAddress> getSeedsAddresses() {
        return seeds.stream().map(ClientDescription::getAddress).collect(Collectors.toSet());
    }

    public void setSeedsAddresses(Set<ClientAddress> seedsAddresses) {
        seeds = seedsAddresses.stream().map(a -> new ClientDescription(null, a)).collect(Collectors.toSet());
    }

    public void addSeed(ClientDescription clientDescription) {
        seeds.add(clientDescription);
    }

    public void removeClient(ClientDescription clientDescription) {
        seeds.remove(clientDescription);
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
        return seeds != null ? seeds.equals(that.seeds) : that.seeds == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (localPath != null ? localPath.hashCode() : 0);
        result = 31 * result + (seeds != null ? seeds.hashCode() : 0);
        return result;
    }
}
