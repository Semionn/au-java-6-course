package com.au.mit.vcs.parser;

import com.au.mit.vcs.common.Repository;
import com.au.mit.vcs.common.commands.ApacheCLIArgs;
import com.au.mit.vcs.common.commands.Command;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.CommandNotFoundException;
import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by semionn on 23.09.16.
 */
public class VCSParser {
    private final Map<String, Command> commands;

    public VCSParser(List<Command> commands) {
        this.commands = commands.stream().collect(Collectors.toMap(Command::getName, Function.identity()));
    }

    public Callable<Void> parse(Repository repository, String[]args) throws CommandBuildingException {
        if (args.length == 0) {
            throw new CommandNotFoundException("Not enough arguments");
        }
        if (!commands.containsKey(args[0])) {
            System.out.println("List of available commands:");
            commands.keySet().forEach(System.out::println);
            throw new CommandNotFoundException(String.format("Command '%s' not found", args[0]));
        }
        Command command = commands.get(args[0]);
        return command.createTask(repository, new ApacheCLIArgs(parse(command, args)));
    }

    private CommandLine parse(Command command, String[] args) {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            String[] trueArgs = Arrays.copyOfRange(args, 1, args.length);
            cmd = parser.parse(command.getOptions(), trueArgs);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(command.getName(), command.getOptions());
            return null;
        }

        return cmd;
    }
}
