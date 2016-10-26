package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Diff;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by semionn on 23.09.16.
 */
public class AddCmd extends Command {
    public AddCmd() {
        super("add", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Path argument expected");
        }

        return () -> {
            addFile(repository, args.get(0));
            return null;
        };
    }

    public static void addFile(Repository repository, String path) {
        try {
            path = repository.makeRelativePath(path);
            repository.getCache().addFile(path);
            repository.getTrackedDiffs().add(new Diff(path, repository.getHead(), false));
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }
}
