package com.au.mit.vcs.common.commit;

import com.au.mit.vcs.common.exceptions.CommandExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.au.mit.vcs.common.Utility.*;

/**
 * Responds to calculating, applying and reverting changes of the selected file
 */
public class Diff implements java.io.Serializable {
    private final String filePath;
    private final Commit previousHead;
    private final boolean deleting;

    /**
     * Diff constructor
     * @param filePath path to file to tracking changes
     * @param previousHead current commit
     * @param deleting is the file should be deleted
     */
    public Diff(String filePath, Commit previousHead, boolean deleting) {
        this.filePath = filePath;
        this.previousHead = previousHead;
        this.deleting = deleting;
    }

    public Path getFilePath() {
        return getCurDirPath().resolve(filePath);
    }

    public String getFileStrPath() {
        return filePath;
    }

    /**
     * returns SHA1 hash of the current time and the tracked file
     */
    public String calcHash() {
        String result = calcSHA1(Long.toString(new java.util.Date().getTime()));
        if (!deleting) {
            result = calcSHA1(calcFileSHA1(getCurrentAbsolutePath(filePath).toString()) + result);
        }
        return result;
    }

    /**
     * Applies stored changes of the tracked file to state of same file in the VCS working directory
     * @param commitPath path to commit folder with stored changes of the tracked file
     */
    public void apply(Path commitPath) {
        try {
            if (deleting) {
                Files.deleteIfExists(getFilePath());
            } else {
                Files.copy(commitPath.resolve(filePath), getFilePath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

    /**
     * Reverts the tracked file to previous state, stored in previous commit
     * @param storagePath path to VCS storage
     */
    public void undo(Path storagePath) {
        try {
            Commit previousFileChangeCommit = previousHead;
            while (!previousFileChangeCommit.getDiffList().stream().anyMatch(diff -> diff.filePath.equals(filePath))) {
                previousFileChangeCommit = previousFileChangeCommit.getPreviousCommit();
                if (previousFileChangeCommit == null) {
                    Files.deleteIfExists(getFilePath());
                    return;
                }
            }
            Path commitPath = storagePath.resolve(previousFileChangeCommit.getHash());
            Files.copy(commitPath.resolve(filePath), getFilePath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

}
