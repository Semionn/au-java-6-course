package com.au.mit.vcs.common;

import com.au.mit.vcs.common.branch.Branch;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.commit.Diff;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import com.au.mit.vcs.common.exceptions.RepositorySerializationException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.au.mit.vcs.common.Utility.calcSHA1;
import static com.au.mit.vcs.common.Utility.getCurDirPath;

/**
 * Primary class of the VCS repository.
 * Stores information about branches, revisions and the index (Cache class)
 * Allows to save meta information in specified file and to load from it
 * The rest of functions implemented in Command classes
 */
public class Repository implements java.io.Serializable {
    private final String storagePath;
    private final static int HASH_LENGTH = 10;
    private final List<Diff> trackedDiffs;
    private final Map<String, Commit> commits;
    private final Map<String, Branch> branches;
    private final Cache cache;
    private Branch currentBranch;
    private Commit head;

    /**
     * Repository constructor.
     * Initializes repository with empty commit in branch "master" and empty index
     * @param storagePath path to saving the repository meta information and the index
     */
    public Repository(Path storagePath) {
        this.storagePath = storagePath.toString();
        trackedDiffs = new ArrayList<>();
        cache = new Cache();
        currentBranch = new Branch("master", null);
        head = new Commit("", "", currentBranch, head, trackedDiffs);
        commits = Arrays.asList(head).stream().collect(Collectors.toMap(Commit::getHash, Function.identity()));
        currentBranch.setLastCommit(head);
        branches = Arrays.asList(currentBranch).stream().collect(Collectors.toMap(Branch::getName, Function.identity()));
    }

    List<Diff> getTrackedDiffs() {
        return trackedDiffs;
    }

    Map<String, Commit> getCommits() {
        return commits;
    }

    Map<String, Branch> getBranches() {
        return branches;
    }

    Branch getCurrentBranch() {
        return currentBranch;
    }

    Commit getHead() {
        return head;
    }

    Cache getCache() {
        return cache;
    }

    void setCurrentBranch(Branch currentBranch) {
        this.currentBranch = currentBranch;
    }

    void setHead(Commit head) {
        this.head = head;
    }

    /**
     * Serializes the specified repository to file with the specified path
     * @param repository the VCS repository
     * @param storagePath path to save
     */
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

    /**
     * Deserializes repository from file with the specified path
     * @param storagePath path to load
     * @return loaded repository
     */
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

    Set<String> getIndexedFiles() {
        Set<String> result = new HashSet<>();
        Commit currentCommit = head;
        while (currentCommit != null) {
            result.addAll(currentCommit.getDiffList().stream()
                    .map(Diff::getFilePath)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList()));
            currentCommit = currentCommit.getPreviousCommit();
        }
        return result;
    }


    Path getCommitPath(String commitHash) {
        return getStoragePath().resolve(commitHash);
    }

    Path getStoragePath() {
        return getCurDirPath().resolve(storagePath);
    }

    static String makeRelativePath(String path) {
        return getCurDirPath().relativize(getCurDirPath().resolve(path).toAbsolutePath()).toString();
    }

    static String getCommitHash(String hash) {
        return calcSHA1(hash).substring(0, HASH_LENGTH);
    }

    boolean isInStoragePath(Path path) {
        String absoluteStoragePath = getStoragePath().toAbsolutePath().toString();
        while (path.getParent() != path &&
                path.getParent() != null &&
                !path.toAbsolutePath().toString().equals(absoluteStoragePath)) {
            if (path.endsWith(storagePath)) {
                return true;
            }
            path = path.getParent();
        }
        return path.endsWith(storagePath);
    }

    private static Path getMetaInfoPath(Path storagePath) {
        return storagePath.resolve("meta");
    }

    class Cache implements java.io.Serializable {
        private final String path = ".cache";
        private final Set<String> files;

        public Cache() {
            files = new HashSet<>();
        }

        public Cache(Set<String> files) {
            this.files = files;
        }

        public boolean containsFile(String filePath) {
            return files.contains(filePath);
        }

        public void addFile(String filePath) throws IOException {
            if (!Files.exists(getCurDirPath().resolve(filePath))) {
                throw new CommandExecutionException(String.format("File '%s' not found", filePath));
            }
            final Path pathToSave = getCachePath().resolve(filePath);
            new File(pathToSave.toString()).getParentFile().mkdirs();
            Files.copy(getCurDirPath().resolve(filePath), pathToSave, StandardCopyOption.REPLACE_EXISTING);
            files.add(filePath);
        }

        public void moveToDir(Path dirPath) throws IOException {
            for (String filePath : files) {
                final Path pathToSave = dirPath.resolve(filePath);
                new File(pathToSave.toString()).getParentFile().mkdirs();
                Files.move(getCachePath().resolve(filePath), pathToSave);
            }
            files.clear();
        }

        public void resetFile(String filePath) throws IOException {
            Files.deleteIfExists(getCachePath().resolve(filePath));
            files.remove(filePath);
        }

        public Path getCachePath() {
            return getStoragePath().resolve(path);
        }

        public Set<String> getFiles() {
            return files;
        }
    }
}
