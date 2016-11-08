package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.au.mit.vcs.common.Utility.getCurDirPath;

/**
 * Corresponds to the VCS command "clean".
 * Allows to remove untracked files from the repository working directory
 */
public class CleanCmd extends Command {
    public CleanCmd() {
        super("clean", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) {
        return () -> {
            clean(repository);
            return null;
        };
    }

    /**
     * Removes untracked files from the repository working directory
     * @param repository the VCS repository
     */
    public static void clean(Repository repository) {
        Set<String> indexedFiles = repository.getCache().getFiles().stream()
                .map(path -> getCurDirPath().resolve(path).toAbsolutePath().toString())
                .collect(Collectors.toSet());
        Commit currentHead = repository.getHead();
        while (currentHead != null) {
            indexedFiles.addAll(currentHead.getDiffMap().keySet().stream()
                    .map(path -> getCurDirPath().resolve(path).toAbsolutePath().toString())
                    .collect(Collectors.toSet()));
            currentHead = currentHead.getPreviousCommit();
        }

        try (Stream<Path> paths = Files.walk(getCurDirPath())) {
            for (Path path : paths.collect(Collectors.toList())) {
                if (!path.toString().equals(".") &&
                        !indexedFiles.contains(path.toString()) &&
                        !repository.isInStoragePath(path) &&
                        !Files.isDirectory(path)) {
                    Files.delete(path);
                }
            }
        } catch (IOException e) {
            throw new CommandExecutionException("Command clean is not performed due to IO error", e);
        }
    }
}
