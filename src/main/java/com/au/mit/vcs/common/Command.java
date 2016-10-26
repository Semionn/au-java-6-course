package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import org.apache.commons.cli.Options;

import java.util.concurrent.Callable;

/**
 * Abstract class for the VCS commands
 * It stores name of command and options for parsing
 */
public abstract class Command {
    private final String name;
    private final Options options;

    public Command(String name, Options options) {
        this.name = name;
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public Options getOptions() {
        return options;
    }

    /**
     * Creates callable task with specified arguments for the command.
     * It will affect on the specified repository
     * @param repository the VCS repository
     * @param commandArgs command arguments
     * @return callable task
     * @throws CommandBuildingException
     */
    public abstract Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException;
}
