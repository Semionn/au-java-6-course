package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import org.apache.commons.cli.Options;

import java.util.concurrent.Callable;

/**
 * Corresponds to the VCS command "log".
 * Allows to log all commits applied in current branch
 */
public class LogCmd extends Command {
    public LogCmd() {
        super("log", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) {
        return () -> {
            printLog(repository);
            return null;
        };
    }

    /**
     * Prints log of all commits applied in current branch
     * @param repository the VCS repository
     */
    public static void printLog(Repository repository) {
        Commit currCommit = repository.getHead();
        while (currCommit.getDepth() != 0) {
            System.out.println(currCommit.print());
            currCommit = currCommit.getPreviousCommit();
        }
    }
}
