package com.au.mit.vcs.common.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by semionn on 23.09.16.
 */
public class ApacheCLIArgs implements CommandArgs {
    private final List<String> args;
    private final Map<String, String> options;

    public ApacheCLIArgs(CommandLine commandLine) {
        args = commandLine.getArgList();
        options = Arrays.asList(commandLine.getOptions()).stream()
                .collect(Collectors.toMap(Option::getLongOpt, Option::getValue));
    }

    @Override
    public List<String> getArgs() {
        return args;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }
}
