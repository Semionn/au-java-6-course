package com.au.mit.vcs;

import com.au.mit.vcs.common.Repository;
import com.au.mit.vcs.common.commands.*;
import com.au.mit.vcs.parser.VCSParser;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by semionn on 22.09.16.
 */
public class VCSApp {
    public static void main(String[] args) throws Exception {
        Repository repository = Repository.deserialize();
        final List<Command> commands = Arrays.asList(
                new AddCmd(),
                new CommitCmd(),
                new BranchCmd(),
                new CheckoutCmd(),
                new MergeCmd(),
                new LogCmd());
        final Callable<Void> task = new VCSParser(commands).parse(repository, args);
        task.call();
        Repository.serialize(repository);
    }
}
