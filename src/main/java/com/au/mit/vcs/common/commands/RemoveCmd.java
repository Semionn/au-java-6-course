package com.au.mit.vcs.common.commands;

import com.au.mit.vcs.common.Repository;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Options;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by semionn on 23.09.16.
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
            repository.removeFile(args.get(0));
            return null;
        };
    }
}
