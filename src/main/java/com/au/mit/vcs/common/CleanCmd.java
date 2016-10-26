package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
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
 * Created by semionn on 23.09.16.
 */
public class CleanCmd extends Command {
    public CleanCmd() {
        super("clean", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        return () -> {
            clean(repository);
            return null;
        };
    }

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
            e.printStackTrace();
        }
    }
}
