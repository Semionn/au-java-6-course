package com.au.mit.vcs.common;

import com.au.mit.vcs.common.command.args.CommandArgs;
import com.au.mit.vcs.common.exceptions.CommandBuildingException;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.au.mit.vcs.common.Utility.getCurDirPath;

/**
 * Created by semionn on 23.09.16.
 */
public class StatusCmd extends Command {
    public StatusCmd() {
        super("status", new Options());
    }

    @Override
    public Callable<Void> createTask(Repository repository, CommandArgs commandArgs) throws CommandBuildingException {
        return () -> {
            printStatus(repository);
            return null;
        };
    }

    public static void printStatus(Repository repository) {
        final Repository.Cache cache = repository.getCache();
        Set<String> indexedFiles = cache.getFiles().stream()
                .map(path -> getCurDirPath().resolve(path).toAbsolutePath().toString())
                .collect(Collectors.toSet());
        Set<String> added = new HashSet<>();
        Set<String> modified = new HashSet<>();
        Set<String> untracked = new HashSet<>();

        try (Stream<Path> paths = Files.walk(getCurDirPath())) {
            for (Path path : paths.collect(Collectors.toList())) {
                if (!path.toString().equals(".") &&
                        !repository.isInStoragePath(path) &&
                        !Files.isDirectory(path)) {
                    if (indexedFiles.contains(path.toString())) {
                        String currentFilePath = path.toString();
                        String cachedFilePath = cache.getCachePath().resolve(getCurDirPath().relativize(path)).toString();
                        if (FileUtils.contentEquals(new File(currentFilePath), new File(cachedFilePath))) {
                            added.add(path.toString());
                        } else {
                            modified.add(path.toString());
                        }
                    } else {
                        untracked.add(path.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Changes to be committed:");
        added.forEach(System.out::println);
        System.out.println("Changes not staged for commit:");
        modified.forEach(System.out::println);
        System.out.println("Untracked files:");
        untracked.forEach(System.out::println);
    }
}
