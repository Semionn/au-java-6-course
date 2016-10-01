package com.au.mit.vcs.common.commands;

import com.au.mit.vcs.common.Repository;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by semionn on 23.09.16.
 */
public class BranchCmd extends Command {
    public BranchCmd() {
        super("branch", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Branch name argument expected");
        }

        return () -> {
            if (commandArgs.getOptions().containsValue("delete")) {
                repository.removeBranch(args.get(0));

            } else {
                repository.makeBranch(args.get(0));
            }
            return null;
        };
    }
}
