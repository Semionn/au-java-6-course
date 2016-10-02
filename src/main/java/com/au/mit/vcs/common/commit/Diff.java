package com.au.mit.vcs.common.commit;

import com.au.mit.vcs.common.exceptions.CommandExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.au.mit.vcs.common.Utility.calcFileSHA1;
import static com.au.mit.vcs.common.Utility.calcSHA1;

/**
 * Created by semionn on 23.09.16.
 */
public class Diff implements java.io.Serializable {
    private final String filePath;
    private final Commit previousHead;
    private final boolean deleting;

    public Diff(String filePath, Commit previousHead, boolean deleting) {
        this.filePath = filePath;
        this.previousHead = previousHead;
        this.deleting = deleting;
    }

    public Path getFilePath() {
        return Paths.get(filePath);
    }

    public String getFileStrPath() {
        return filePath;
    }

    public String calcHash() {
        String result = calcSHA1(Long.toString(new java.util.Date().getTime()));
        if (!deleting) {
            result = calcSHA1(calcFileSHA1(filePath) + result);
        }
        return result;
    }

    public void apply(Path commitPath) {
        try {
            if (deleting) {
                Files.deleteIfExists(getFilePath());
            } else {
                Files.copy(commitPath.resolve(getFilePath()), getFilePath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

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
            Files.copy(commitPath.resolve(getFilePath()), getFilePath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

}
