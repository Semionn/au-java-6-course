package com.au.mit.vcs.common;

import com.au.mit.vcs.common.branch.Branch;
import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.commit.Diff;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Options;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.au.mit.vcs.common.Utility.calcFileSHA1;

/**
 * Corresponds to the VCS command "merge".
 * Allows to merge commits from specified branch to current branch
 */
public class MergeCmd extends Command {
    public MergeCmd() {
        super("merge", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Branch name argument expected");
        }

        return () -> {
            merge(repository, args.get(0));
            return null;
        };
    }

    /**
     * Merges commits from the specified branch to the current branch
     * @param repository the VCS repository
     * @param branchName name of merged branch
     */
    public static void merge(Repository repository, String branchName) {
        if (!repository.getBranches().containsKey(branchName)) {
            System.out.println(String.format("Branch '%s' not found", branchName));
            return;
        }

        final Branch mergedBranch = repository.getBranches().get(branchName);
        final Commit mergedHead = mergedBranch.getLastCommit();
        final Map<String, Diff> totalDiff = new HashMap<>();
        final Map<String, Commit> diffCommits = new HashMap<>();

        Commit updateCommit = mergedHead;
        Commit oldCommit = repository.getHead();
        while (oldCommit.getDepth() > updateCommit.getDepth()) {
            oldCommit = oldCommit.getPreviousCommit();
        }

        while (oldCommit != updateCommit) {
            updateCommit.getDiffMap().forEach(totalDiff::putIfAbsent);
            for (String filePath : updateCommit.getDiffMap().keySet()) {
                diffCommits.putIfAbsent(filePath, updateCommit);
            }
            if (oldCommit.getDepth() == updateCommit.getDepth()) {
                oldCommit = oldCommit.getPreviousCommit();
            }
            updateCommit = updateCommit.getPreviousCommit();
        }

        Set<String> trackedFiles = new HashSet<>(repository.getTrackedDiffs().stream()
                .map(Diff::getFileStrPath).collect(Collectors.toList()));
        Set<String> conflictedFiles = new HashSet<>();
        for (Map.Entry<String, Commit> entry : diffCommits.entrySet()) {
            String filePath = entry.getKey();
            Path commitPath = repository.getCommitPath(entry.getValue().getHash());
            if (trackedFiles.contains(filePath)) {
                String mergedHash = calcFileSHA1(commitPath.resolve(filePath).toString());
                String currentHash = calcFileSHA1(filePath);
                if (!mergedHash.equals(currentHash)) {
                    conflictedFiles.add(filePath);
                }
            }
        }
        if (!conflictedFiles.isEmpty()) {
            System.out.println("Conflicts occurred during merge in files:");
            conflictedFiles.forEach(System.out::println);
            return;
        }

        for (Diff diff : totalDiff.values()) {
            Path commitPath = repository.getCommitPath(diffCommits.get(diff.getFileStrPath()).getHash());
            diff.apply(commitPath);
        }

        totalDiff.forEach((s, diff) -> repository.getTrackedDiffs().add(new Diff(s, repository.getHead(), false)));
        CommitCmd.makeCommit(repository,
                String.format("Merged from '%s' to '%s'", branchName, repository.getCurrentBranch().getName()));
    }
}
