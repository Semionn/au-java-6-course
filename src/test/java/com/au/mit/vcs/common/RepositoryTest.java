package com.au.mit.vcs.common;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Created by semionn on 25.09.16.
 */
public class RepositoryTest {

    @Test
    public void testMakeCommit() throws Exception {
        final Path storagePath = Paths.get(".vcs_test");
        File temp = new File(Files.createTempFile(Paths.get("."), "test.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            String commitMessage = "message";
            addFile(repository, temp, "message");
            checkLog(repository, new String[]{commitMessage});
        } finally {
            Files.deleteIfExists(temp.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }

    }

    @Test
    public void testCheckout() throws Exception {
        final Path storagePath = Paths.get(".vcs_test");
        File temp1 = new File(Files.createTempFile(Paths.get("."), "test.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(Paths.get("."), "test2.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            addFile(repository, temp1, "message1");

            String branchName = "new-branch";
            repository.makeBranch(branchName);
            addFile(repository, temp2, "message2");

            checkLog(repository, new String[]{"message2", "message1"});
            repository.checkout(branchName);
            checkLog(repository, new String[]{"message1"});
            repository.checkout("master");
            checkLog(repository, new String[]{"message2", "message1"});
        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testRemoveBranch() throws Exception {
        final Path storagePath = Paths.get(".vcs_test");
        File temp1 = new File(Files.createTempFile(Paths.get("."), "test.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(Paths.get("."), "test2.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            addFile(repository, temp1, "message1");

            String branchName = "new-branch";
            repository.makeBranch(branchName);
            addFile(repository, temp2, "message2");

            checkLog(repository, new String[]{"message2", "message1"});
            repository.checkout(branchName);
            checkLog(repository, new String[]{"message1"});
            repository.checkout("master");
            checkLog(repository, new String[]{"message2", "message1"});
            repository.removeBranch(branchName);
            checkOutput(() -> {
                repository.checkout(branchName);
                return null;
            }, String.format("Branch or revision '%s' not found" + getEndLine(), branchName));

        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testMerge() throws Exception {
        final Path storagePath = Paths.get(".vcs_test");
        File temp1 = new File(Files.createTempFile(Paths.get("."), "test.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(Paths.get("."), "test2.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            addFile(repository, temp1, "message1");

            String branchName = "new-branch";
            repository.makeBranch(branchName);
            addFile(repository, temp2, "message2");
            repository.checkout(branchName);
            repository.merge("master");
            checkLog(repository, new String[]{"Merged from 'master' to 'new-branch'", "message1"});
        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    private void addFile(Repository repository, File temp, String commitMessage) throws IOException {
        String tempFileText = "text";
        repository.trackFile(temp.getPath());
        try (FileOutputStream fs = new FileOutputStream(temp)) {
            fs.write(tempFileText.getBytes());
        }
        repository.makeCommit(commitMessage);
    }

    private void checkLog(Repository repository, String[] logMessages) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             PrintStream out = new PrintStream(byteArrayOutputStream, true, "UTF-8")) {

            try {
                System.setOut(out);
                repository.printLog();
            } finally {
                System.setOut(System.out);
            }

            final String shellOutput = byteArrayOutputStream.toString("UTF-8");
            final String[] outputLines = shellOutput.split(getEndLine());
            int checkedMessagesCount = 0;
            for (int i = 0; i < outputLines.length; i++) {
                final String[] splitted = outputLines[i].split(": ");
                if (splitted.length > 1) {
                    assertEquals(splitted[1], logMessages[i]);
                    checkedMessagesCount++;
                }
            }
            assertEquals(checkedMessagesCount, logMessages.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkOutput(Callable<Void> action, String rightLog) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             PrintStream out = new PrintStream(byteArrayOutputStream, true, "UTF-8")) {

            try {
                System.setOut(out);
                action.call();
            } finally {
                System.setOut(System.out);
            }

            final String shellOutput = byteArrayOutputStream.toString("UTF-8");
            assertEquals(rightLog, shellOutput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getEndLine() {
        return System.getProperty("line.separator");
    }
}