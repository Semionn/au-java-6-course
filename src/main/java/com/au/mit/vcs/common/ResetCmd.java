package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Corresponds to the VCS command "reset".
 * Allows to remove file from the repository index
 */
public class ResetCmd extends Command {
    public ResetCmd() {
        super("reset", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws NotEnoughArgumentsException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Path argument expected");
        }

        return () -> {
            resetFile(repository, args.get(0));
            return null;
        };
    }

    /**
     * Reset state of file in the VCS working directory to state of the last commit
     * @param repository the VCS repository
     * @param filePath path of the file in the working directory
     */
    public static void resetFile(Repository repository, String filePath) {
        try {
            String relativeFilePath = Repository.makeRelativePath(filePath);
            repository.getCache().resetFile(relativeFilePath);
            final Map<String, Commit> indexedFiles = repository.getIndexedFiles();
            repository.getTrackedDiffs().stream()
                    .filter(diff -> diff.getFileStrPath().equals(relativeFilePath))
                    .collect(Collectors.toList())
                    .forEach(diff -> {
                        if (indexedFiles.containsKey(relativeFilePath)) {
                            diff.undo(repository.getStoragePath());
                        }
                        repository.getTrackedDiffs().remove(diff);
                    });
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }
}
