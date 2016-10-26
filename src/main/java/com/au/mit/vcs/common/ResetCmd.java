package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.List;
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
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Path argument expected");
        }

        return () -> {
            resetFile(repository, args.get(0));
            return null;
        };
    }

    public static void resetFile(Repository repository, String filePath) {
        try {
            String relativeFilePath = repository.makeRelativePath(filePath);
            repository.getCache().resetFile(relativeFilePath);
            repository.getTrackedDiffs().stream()
                    .filter(diff -> diff.getFileStrPath().equals(relativeFilePath))
                    .collect(Collectors.toList())
                    .forEach(repository.getTrackedDiffs()::remove);
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }
}
