package com.au.mit.vcs.common.commands;

import com.au.mit.vcs.common.Repository;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by semionn on 23.09.16.
 */
public class CheckoutCmd extends Command {
    public CheckoutCmd() {
        super("checkout", generateOptions());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Branch name argument expected");
        }

        String branchName = args.get(0);

        return () -> {
            if (commandArgs.getOptions().containsKey("delete")) {
                repository.removeBranch(branchName);
            } else {
                repository.checkout(branchName);
            }
            return null;
        };
    }

    private static Options generateOptions() {
        Options options = new Options();
        Option msg = new Option("d", "delete", false, "delete branch");
        msg.setRequired(false);
        options.addOption(msg);
        return options;
    }
}
