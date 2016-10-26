package com.au.mit.vcs.common;

import com.au.mit.vcs.common.branch.Branch;
import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.commit.Diff;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by semionn on 23.09.16.
 */
public class CheckoutCmd extends Command {
    public CheckoutCmd() {
        super("checkout", generateOptions());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Branch name argument expected");
        }

        String branchName = args.get(0);

        return () -> {
            if (commandArgs.getOptions().containsKey("delete")) {
                BranchCmd.removeBranch(repository, branchName);
            } else {
                checkout(repository, branchName);
            }
            return null;
        };
    }

    public static void checkout(Repository repository, String target) {
        if (!repository.getTrackedDiffs().isEmpty()) {
            System.out.println("There are uncommitted changes");
            return;
        }
        if (repository.getHead().getHash().equals(target)) {
            System.out.println("Already at that revision");
            return;
        }
        Branch newBranch = null;
        Commit newHead = null;

        final Map<String, Commit> commits = repository.getCommits();
        final Map<String, Branch> branches = repository.getBranches();
        if (!commits.containsKey(target)) {
            if (!branches.containsKey(target)) {
                System.out.println(String.format("Branch or revision '%s' not found", target));
                return;
            } else {
                newBranch = branches.get(target);
                newHead = newBranch.getLastCommit();
            }
        }
        if (newHead == null) {
            newHead = commits.get(target);
            newBranch = newHead.getBranch();
        }

        checkoutToRevision(repository, newHead);
        repository.setCurrentBranch(newBranch);
        repository.setHead(newHead);
    }

    private static void checkoutToRevision(Repository repository, Commit newHead) {
        Map<String, Diff> totalDiff = new HashMap<>();
        Map<String, Commit> diffCommits = new HashMap<>();

        Commit updateCommit = newHead;
        Commit oldCommit = repository.getHead();
        while (oldCommit.getDepth() > updateCommit.getDepth()) {
            for (Diff diff : oldCommit.getDiffList()) {
                diff.undo(repository.getStoragePath());
            }
            oldCommit = oldCommit.getPreviousCommit();
        }

        while (oldCommit != updateCommit) {
            updateCommit.getDiffMap().forEach(totalDiff::putIfAbsent);
            for (String filePath : updateCommit.getDiffMap().keySet()) {
                diffCommits.putIfAbsent(filePath, updateCommit);
            }
            if (oldCommit.getDepth() == updateCommit.getDepth()) {
                for (Diff diff : oldCommit.getDiffList()) {
                    diff.undo(repository.getStoragePath());
                }
                oldCommit = oldCommit.getPreviousCommit();
            }
            updateCommit = updateCommit.getPreviousCommit();
        }

        for (Diff diff : totalDiff.values()) {
            Path commitPath = repository.getCommitPath(diffCommits.get(diff.getFileStrPath()).getHash());
            diff.apply(commitPath);
        }
    }

    private static Options generateOptions() {
        Options options = new Options();
        Option msg = new Option("d", "delete", false, "delete branch");
        msg.setRequired(false);
        options.addOption(msg);
        return options;
    }
}
