package com.au.mit.vcs.common;

import com.au.mit.vcs.common.branch.Branch;
import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.commit.Diff;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Corresponds to the VCS command "commit".
 * Allows to commit tracked changes
 */
public class CommitCmd extends Command {
    private static final Logger logger = Logger.getLogger(CommitCmd.class.getName());

    public CommitCmd() {
        super("commit", generateOptions());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) {
        final Map<String, String> options = commandArgs.getOptions();

        return () -> {
            makeCommit(repository, options.get("message"));
            return null;
        };
    }

    /**
     * Makes commit in specified repository with specified message
     * It fixes changes, stored in the repository index
     * @param repository the VCS repository
     * @param message the commit message
     */
    public static void makeCommit(Repository repository, String message) {
        final List<Diff> trackedDiffs = repository.getTrackedDiffs();
        if (trackedDiffs.isEmpty()) {
            System.out.println("No changes to commit");
            return;
        }
        try {
            final StringBuilder hash = new StringBuilder();
            for (Diff diff : trackedDiffs) {
                hash.append(diff.calcHash());
            }
            final String commitHash = Repository.getCommitHash(hash.toString());
            final Path commitFolder = repository.getCommitPath(commitHash);
            Files.createDirectories(commitFolder);

            repository.getCache().moveToDir(commitFolder);

            final Branch currentBranch = repository.getCurrentBranch();
            Commit head = repository.getHead();
            head = new Commit(commitHash, message, currentBranch, head, trackedDiffs);
            repository.setHead(head);
            repository.getCommits().put(commitHash, head);
            currentBranch.setLastCommit(head);
            trackedDiffs.clear();
            final String logMsg = String.format("Committed successfully: %s", commitHash);
            System.out.println(logMsg);
            logger.info(logMsg);
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

    private static Options generateOptions() {
        Options options = new Options();
        Option msg = new Option("m", "message", true, "commit message");
        msg.setRequired(true);
        options.addOption(msg);
        return options;
    }
}
