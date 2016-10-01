package com.au.mit.vcs.common;

import com.au.mit.vcs.common.branch.Branch;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.commit.Diff;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import com.au.mit.vcs.common.exceptions.RepositorySerializationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.au.mit.vcs.common.Utility.calcFileSHA1;
import static com.au.mit.vcs.common.Utility.calcSHA1;

/**
 * Created by semionn on 22.09.16.
 */
public class Repository implements java.io.Serializable {
    private final String storagePath;
    private final static int HASH_LENGTH = 10;
    private final List<Diff> trackedDiffs;
    private final Map<String, Commit> commits;
    private final Map<String, Branch> branches;
    private Branch currentBranch;
    private Commit head;

    public Repository(Path storagePath) {
        this.trackedDiffs = new ArrayList<>();
        this.storagePath = storagePath.toString();
        currentBranch = new Branch("master", null);
        head = new Commit("", "", currentBranch, head, trackedDiffs);
        commits = Arrays.asList(head).stream().collect(Collectors.toMap(Commit::getHash, Function.identity()));
        currentBranch.setLastCommit(head);
        branches = Arrays.asList(currentBranch).stream().collect(Collectors.toMap(Branch::getName, Function.identity()));
    }

    public Repository(Commit head, Branch currentBranch, Map<String, Branch> branches, Map<String, Commit> commits,
                      List<Diff> trackedDiffs, Path storagePath) {
        this.head = head;
        this.currentBranch = currentBranch;
        this.branches = branches;
        this.commits = commits;
        this.trackedDiffs = trackedDiffs;
        this.storagePath = storagePath.toString();
    }

    public void trackFile(String path) {
        if (!Files.exists(Paths.get(path))) {
            throw new CommandExecutionException(String.format("File '%s' not found", path));
        }

        for (Diff diff : trackedDiffs) {
            if (diff.getFilePath().equals(Paths.get(path))) {
                return;
            }
        }
        trackedDiffs.add(new Diff(path, head));
    }

    public void makeCommit(String message) {
        if (trackedDiffs.size() == 0) {
            System.out.println("No changes to commit");
            return;
        }
        try {
            StringBuilder hash = new StringBuilder();
            for (Diff diff : trackedDiffs) {
                hash.append(diff.calcHash());
            }
            final String commitHash = getCommitHash(hash.toString());
            final Path commitFolder = getCommitPath(commitHash);
            Files.createDirectories(commitFolder);

            for (Diff diff : trackedDiffs) {
                final Path pathToSave = commitFolder.resolve(diff.getFilePath());
                new File(pathToSave.toString()).getParentFile().mkdirs();
                Files.copy(diff.getFilePath(), pathToSave);
            }

            head = new Commit(commitHash, message, currentBranch, head, trackedDiffs);
            commits.put(commitHash, head);
            currentBranch.setLastCommit(head);
            trackedDiffs.clear();
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

    public void makeBranch(String branchName) {
        Branch newBranch = new Branch(branchName, head);
        branches.put(branchName, newBranch);
    }

    public void removeBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println(String.format("Branch '%s' not found", branchName));
            return;
        }

        if (currentBranch.getName().equals(branchName)) {
            System.out.println("Cannot remove current branch");
            return;
        }

        try {
            branches.get(branchName).markDeleted();
            for (Branch branch: branches.values()) {
                if (branch.possibleToDelete()) {
                    Commit lastCommit = branch.getLastCommit();
                    while (lastCommit.getBranch() == branch) {
                        Files.deleteIfExists(getCommitPath(lastCommit.getHash()));
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

    public void checkout(String target) {
        if (!trackedDiffs.isEmpty()) {
            System.out.println("There are uncommitted changes");
            return;
        }
        if (head.getHash().equals(target)) {
            System.out.println("Already at that revision");
            return;
        }
        Branch newBranch = null;
        Commit newHead = null;
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

        checkoutToRevision(newHead);
        currentBranch = newBranch;
        head = newHead;
    }

    private void checkoutToRevision(Commit newHead) {
        Map<String, Diff> totalDiff = new HashMap<>();
        Map<String, Commit> diffCommits = new HashMap<>();

        Commit updateCommit = newHead;
        Commit oldCommit = head;
        while (oldCommit.getDepth() > updateCommit.getDepth()) {
            for (Diff diff : oldCommit.getDiffList()) {
                diff.undo(Paths.get(storagePath));
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
                    diff.undo(Paths.get(storagePath));
                }
                oldCommit = oldCommit.getPreviousCommit();
            }
            updateCommit = updateCommit.getPreviousCommit();
        }

        for (Diff diff : totalDiff.values()) {
            Path commitPath = getCommitPath(diffCommits.get(diff.getFileStrPath()).getHash());
            diff.apply(commitPath);
        }
    }

    public void merge(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println(String.format("Branch '%s' not found", branchName));
            return;
        }

        Branch mergedBranch = branches.get(branchName);
        Commit mergedHead = mergedBranch.getLastCommit();
        Map<String, Diff> totalDiff = new HashMap<>();
        Map<String, Commit> diffCommits = new HashMap<>();

        Commit updateCommit = mergedHead;
        Commit oldCommit = head;
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

        Set<String> trackedFiles = new HashSet<>(trackedDiffs.stream().map(Diff::getFileStrPath).collect(Collectors.toList()));
        Set<String> conflictedFiles = new HashSet<>();
        for (Map.Entry<String, Commit> entry : diffCommits.entrySet()) {
            String filePath = entry.getKey();
            Path commitPath = getCommitPath(entry.getValue().getHash());
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
            Path commitPath = getCommitPath(diffCommits.get(diff.getFileStrPath()).getHash());
            diff.apply(commitPath);
        }

        totalDiff.forEach((s, diff) -> trackedDiffs.add(new Diff(s, head)));
        makeCommit(String.format("Merged from '%s' to '%s'", branchName, currentBranch.getName()));
    }

    public void printLog() {
        Commit currCommit = head;
        while (currCommit.getDepth() != 0) {
            System.out.println(currCommit.print());
            currCommit = currCommit.getPreviousCommit();
        }
    }

    public static void serialize(Repository repository, Path storagePath) {
        try {
            new File(storagePath.toString()).mkdir();
            FileOutputStream fileOut =
                    new FileOutputStream(getMetaInfoPath(storagePath).toString());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(repository);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            throw new RepositorySerializationException(e);
        }
    }

    public static Repository deserialize(Path storagePath) {
        Repository repository;
        try {
            FileInputStream fileIn = new FileInputStream(getMetaInfoPath(storagePath).toString());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            repository = (Repository) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            return new Repository(storagePath);
        }
        return repository;
    }

    private static Path getMetaInfoPath(Path storagePath) {
        return storagePath.resolve("meta");
    }

    private Path getCommitPath(String commitHash) {
        return Paths.get(storagePath).resolve(commitHash);
    }

    private static String getCommitHash(String hash) {
        return calcSHA1(hash).substring(0, HASH_LENGTH);
    }
}
