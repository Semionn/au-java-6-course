package com.au.mit.vcs.common.commands;

import com.au.mit.vcs.common.Repository;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import org.apache.commons.cli.Options;

import java.util.concurrent.Callable;

/**
 * Created by semionn on 23.09.16.
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

    public abstract Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException;
}
