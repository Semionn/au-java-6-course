package com.au.mit.torrent.common;

import java.util.Set;

public class PeerFileStat {
    public final static int PART_SIZE = 4096;

    private final int fileID;
    private final Set<Integer> parts;

    public PeerFileStat(int fileID, Set<Integer> parts) {
        this.fileID = fileID;
        this.parts = parts;
    }

    public int getFileID() {
        return fileID;
    }

    public Set<Integer> getParts() {
        return parts;
    }

    public static int getPartPosition(int partNum) {
        return partNum * PART_SIZE;
    }

    public static int getPartsCount(long fileSize) {
        return (int) (fileSize / PART_SIZE) + (fileSize % PART_SIZE > 0 ? 1 : 0);
    }

    public static int calcPartSize(int partNum, long fileSize) {
        if (getPartsCount(fileSize) == partNum + 1) {
            return (int) (fileSize - (getPartsCount(fileSize) - 1) * PART_SIZE);
        }
        return PART_SIZE;
    }
}
