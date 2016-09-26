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

    public Diff(String filePath, Commit previousHead) {
        this.filePath = filePath;
        this.previousHead = previousHead;
    }

    public Path getFilePath() {
        return Paths.get(filePath);
    }

    public String getFileStrPath() {
        return filePath;
    }

    public String calcHash() {
        return calcSHA1(calcFileSHA1(filePath) + calcSHA1(Long.toString(new java.util.Date().getTime())));
    }

    public void apply(Path commitPath) {
        try {
            Files.copy(commitPath.resolve(getFilePath()), getFilePath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

    public void undo(Path storagePath) {
        try {
            Commit previousFileChangeCommit = previousHead;
            while (!previousFileChangeCommit.getDiffList().contains(filePath)) {
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
