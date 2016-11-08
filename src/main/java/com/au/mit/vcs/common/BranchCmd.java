package com.au.mit.vcs.common;

import com.au.mit.vcs.common.branch.Branch;
import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import com.au.mit.vcs.common.exceptions.NotEnoughArgumentsException;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Corresponds to the VCS commands "branch -delete branch_name" and "branch branch_name".
 * Allows creating and deleting of the VCS branches
 */
public class BranchCmd extends Command {
    public BranchCmd() {
        super("branch", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws NotEnoughArgumentsException {
        final List<String> args = commandArgs.getArgs();
        if (args.size() == 0) {
            throw new NotEnoughArgumentsException("Branch name argument expected");
        }

        return () -> {
            if (commandArgs.getOptions().containsValue("delete")) {
                removeBranch(repository, args.get(0));
            } else {
                makeBranch(repository, args.get(0));
            }
            return null;
        };
    }

    /**
     * Makes brunch in specified repository with specified name
     * @param repository the VCS repository
     * @param branchName name of the branch
     */
    public static void makeBranch(Repository repository, String branchName) {
        if (repository.getBranches().containsKey(branchName)) {
            System.out.println(String.format("Branch with name '%s' already exists", branchName));
            return;
        }
        Branch newBranch = new Branch(branchName, repository.getHead());
        repository.getBranches().put(branchName, newBranch);
    }

    /**
     * Removes brunch from specified repository with specified name
     * @param repository the VCS repository
     * @param branchName name of the branch
     */
    public static void removeBranch(Repository repository, String branchName) {
        final Map<String, Branch> branches = repository.getBranches();
        if (!branches.containsKey(branchName)) {
            throw new CommandExecutionException(String.format("Branch '%s' not found", branchName));
        }

        if (repository.getCurrentBranch().getName().equals(branchName)) {
            throw new CommandExecutionException("Cannot remove current branch");
        }

        try {
            branches.get(branchName).markDeleted();
            for (Branch branch : branches.values()) {
                if (branch.possibleToDelete()) {
                    Commit lastCommit = branch.getLastCommit();
                    while (lastCommit.getBranch() == branch) {
                        Files.deleteIfExists(repository.getCommitPath(lastCommit.getHash()));
                        lastCommit = lastCommit.getPreviousCommit();
                    }
                }
            }
            branches.values().stream().filter(Branch::possibleToDelete).collect(Collectors.toList())
                    .forEach(branch -> branches.remove(branch.getName()));
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }
}
