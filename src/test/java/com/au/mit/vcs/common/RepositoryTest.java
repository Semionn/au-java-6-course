package com.au.mit.vcs.common;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import static com.au.mit.vcs.common.Utility.getCurDirPath;

import static org.junit.Assert.*;

/**
 * Created by semionn on 25.09.16.
 */
public class RepositoryTest {

    @Before
    public void changeDir() throws IOException {
        final Path tempDirectoryPath = Files.createTempDirectory("");
        System.setProperty("user.dir", tempDirectoryPath.toAbsolutePath().toString());
    }

    @Test
    public void testMakeCommit() throws Exception {
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp = new File(Files.createTempFile(getCurDirPath(), "test.tmp", "").toString());
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
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp1 = new File(Files.createTempFile(getCurDirPath(), "test.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(getCurDirPath(), "test2.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            addFile(repository, temp1, "message1");

            String branchName = "new-branch";
            BranchCmd.makeBranch(repository, branchName);
            addFile(repository, temp2, "message2");

            checkLog(repository, new String[]{"message2", "message1"});
            CheckoutCmd.checkout(repository, branchName);
            checkLog(repository, new String[]{"message1"});
            CheckoutCmd.checkout(repository, "master");
            checkLog(repository, new String[]{"message2", "message1"});
        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testRemoveBranch() throws Exception {
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp1 = new File(Files.createTempFile(getCurDirPath(), "test.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(getCurDirPath(), "test2.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            addFile(repository, temp1, "message1");

            String branchName = "new-branch";
            BranchCmd.makeBranch(repository, branchName);
            addFile(repository, temp2, "message2");

            checkLog(repository, new String[]{"message2", "message1"});
            CheckoutCmd.checkout(repository, branchName);
            checkLog(repository, new String[]{"message1"});
            CheckoutCmd.checkout(repository, "master");
            checkLog(repository, new String[]{"message2", "message1"});
            BranchCmd.removeBranch(repository, branchName);
            checkOutput(() -> {
                CheckoutCmd.checkout(repository, branchName);
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
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp1 = new File(Files.createTempFile(getCurDirPath(), "test.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(getCurDirPath(), "test2.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            addFile(repository, temp1, "message1");

            String branchName = "new-branch";
            BranchCmd.makeBranch(repository, branchName);
            addFile(repository, temp2, "message2");
            CheckoutCmd.checkout(repository, branchName);
            MergeCmd.merge(repository, "master");
            checkLog(repository, new String[]{"Merged from 'master' to 'new-branch'", "message1"});
        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testReset() throws Exception {
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp = new File(Files.createTempFile(getCurDirPath(), "test.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            String commitMessage = "message";
            AddCmd.addFile(repository, temp.getPath());
            ResetCmd.resetFile(repository, temp.getPath());
            checkOutput(() -> {
                CommitCmd.makeCommit(repository, commitMessage);
                return null;
            }, "No changes to commit" + getEndLine());
            addFile(repository, temp, commitMessage);
            checkLog(repository, new String[]{commitMessage});
        } finally {
            Files.deleteIfExists(temp.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testRemove() throws Exception {
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp = new File(Files.createTempFile(getCurDirPath(), "test.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            String commitMessage = "message";
            addFile(repository, temp, commitMessage);
            RemoveCmd.removeFile(repository, temp.getPath());
            checkOutput(() -> {
                CommitCmd.makeCommit(repository, commitMessage);
                return null;
            }, "Committed successfully");
            final List<String> commitHashes = checkLog(repository, new String[]{commitMessage, commitMessage});
            CheckoutCmd.checkout(repository, commitHashes.get(1));
            checkLog(repository, new String[]{commitMessage});
            assertTrue(Files.exists(temp.toPath()));
            CheckoutCmd.checkout(repository, commitHashes.get(0));
            assertFalse(Files.exists(temp.toPath()));
        } finally {
            Files.deleteIfExists(temp.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testClean() throws Exception {
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp1 = new File(Files.createTempFile(getCurDirPath(), "test1.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(getCurDirPath(), "test2.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            String commitMessage = "message";
            addFile(repository, temp1, commitMessage);
            CleanCmd.clean(repository);
            assertFalse(Files.exists(temp2.toPath()));
        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testStatus() throws Exception {
        final Path storagePath = getCurDirPath().resolve(".vcs_test");
        File temp1 = new File(Files.createTempFile(getCurDirPath(), "test1.tmp", "").toString());
        File temp2 = new File(Files.createTempFile(getCurDirPath(), "test2.tmp", "").toString());
        File temp3 = new File(Files.createTempFile(getCurDirPath(), "test3.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            AddCmd.addFile(repository, temp1.getPath());
            AddCmd.addFile(repository, temp2.getPath());
            try (FileOutputStream fs = new FileOutputStream(temp2)) {
                fs.write("new text".getBytes());
            }
            checkOutput(() -> {
                StatusCmd.printStatus(repository);
                return null;
            }, "Changes to be committed:" + getEndLine() +
                    temp1.getAbsolutePath() + getEndLine() +
                    "Changes not staged for commit:" + getEndLine() +
                    temp2.getAbsolutePath() + getEndLine() +
                    "Untracked files:" + getEndLine() +
                    temp3.getAbsolutePath() + getEndLine());
        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            Files.deleteIfExists(temp3.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    private void addFile(Repository repository, File temp, String commitMessage) throws IOException {
        String tempFileText = "text";
        AddCmd.addFile(repository, temp.getPath());
        try (FileOutputStream fs = new FileOutputStream(temp)) {
            fs.write(tempFileText.getBytes());
        }
        CommitCmd.makeCommit(repository, commitMessage);
    }

    private List<String> checkLog(Repository repository, String[] logMessages) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             PrintStream out = new PrintStream(byteArrayOutputStream, true, "UTF-8")) {

            try {
                System.setOut(out);
                LogCmd.printLog(repository);
            } finally {
                System.setOut(System.out);
            }

            final String shellOutput = byteArrayOutputStream.toString("UTF-8");
            final String[] outputLines = shellOutput.split(getEndLine());
            final List<String> commitHashes = new ArrayList<>();
            int checkedMessagesCount = 0;
            for (int i = 0; i < outputLines.length; i++) {
                final String[] splitted = outputLines[i].split(": ");
                if (splitted.length > 1) {
                    commitHashes.add(splitted[0]);
                    assertEquals(splitted[1], logMessages[i]);
                    checkedMessagesCount++;
                }
            }
            assertEquals(checkedMessagesCount, logMessages.length);
            return commitHashes;
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
            assertEquals(rightLog, shellOutput.substring(0, rightLog.length()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getEndLine() {
        return System.getProperty("line.separator");
    }
}