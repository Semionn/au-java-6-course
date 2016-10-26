package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Diff;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

import static com.au.mit.vcs.common.Utility.getCurDirPath;

/**
 * Corresponds to the VCS command "remove".
 * Allows to remove file from the VCS working directory and after commit this change
 */
public class RemoveCmd extends Command {
    public RemoveCmd() {
        super("rm", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Path argument expected");
        }

        return () -> {
            removeFile(repository, args.get(0));
            return null;
        };
    }

    /**
     * Removes file from the VCS working directory.
     * This changing could be stored in the next commit
     * @param repository the VCS repository
     * @param filePath path to the file to remove
     */
    public static void removeFile(Repository repository, String filePath) {
        filePath = repository.makeRelativePath(filePath);
        if (!repository.getCache().containsFile(filePath) && !Files.exists(getCurDirPath().resolve(filePath))) {
            throw new CommandExecutionException(String.format("File '%s' not found in the index", filePath));
        }

        try {
            ResetCmd.resetFile(repository, filePath);
            repository.getTrackedDiffs().add(new Diff(filePath, repository.getHead(), true));
            Files.deleteIfExists(getCurDirPath().resolve(filePath));
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }
}
