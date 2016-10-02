package com.au.mit.vcs.common.commands;

import com.au.mit.vcs.common.Repository;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import org.apache.commons.cli.Options;

import java.util.concurrent.Callable;

/**
 * Created by semionn on 23.09.16.
 */
public class StatusCmd extends Command {
    public StatusCmd() {
        super("status", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        return () -> {
            repository.printStatus();
            return null;
        };
    }
}
