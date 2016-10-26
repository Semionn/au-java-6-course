package com.au.mit.vcs;

import com.au.mit.vcs.common.*;
import com.au.mit.vcs.parser.VCSParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Console application for the VCS repository
 */
public class VCSApp {

    /**
     * Starts the VCS application.
     * Path to repository: .vcs
     * After command execution application will be closed
     * @param args the VCS command and its arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Path storagePath = Paths.get(".vcs");
        Repository repository = Repository.deserialize(storagePath);
        final List<Command> commands = Arrays.asList(
                new AddCmd(),
                new CommitCmd(),
                new BranchCmd(),
                new CheckoutCmd(),
                new MergeCmd(),
                new LogCmd(),
                new ResetCmd(),
                new RemoveCmd(),
                new CleanCmd(),
                new StatusCmd());
        final Callable<Void> task = new VCSParser(commands).parse(repository, args);
        task.call();
        Repository.serialize(repository, storagePath);
    }
}
