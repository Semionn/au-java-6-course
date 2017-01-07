package com.au.mit.torrent.common;

import java.util.Set;

/**
 * Class for storing information about available parts for file with specified ID
 */
public class PeerFileStat {
    public final static int PART_SIZE = 32 * 1024;

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

    /**
     * Returns position of part of the file by number of the part
     */
    public static int getPartPosition(int partNum) {
        return partNum * PART_SIZE;
    }

    /**
     * Returns count of parts of file with specified size
     */
    public static int getPartsCount(long fileSize) {
        return (int) (fileSize / PART_SIZE) + (fileSize % PART_SIZE > 0 ? 1 : 0);
    }

    /**
     * Returns PART_SIZE if the part is not last in the file, and the rest length otherwise
     */
    public static int calcPartSize(int partNum, long fileSize) {
        if (getPartsCount(fileSize) == partNum + 1) {
            return (int) (fileSize - (getPartsCount(fileSize) - 1) * PART_SIZE);
        }
        return PART_SIZE;
    }
}
