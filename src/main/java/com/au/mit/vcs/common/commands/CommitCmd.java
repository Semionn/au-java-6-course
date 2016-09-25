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
public class CommitCmd extends Command {
    public CommitCmd() {
        super("commit", generateOptions());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final Map<String, String> options = commandArgs.getOptions();

        return () -> {
            repository.makeCommit(options.get("message"));
            return null;
        };
    }

    private static Options generateOptions() {
        Options options = new Options();
        Option msg = new Option("m", "message", true, "commit message");
        msg.setRequired(true);
        options.addOption(msg);
        return options;
    }
}
