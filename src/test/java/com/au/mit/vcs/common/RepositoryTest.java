package com.au.mit.vcs.common;

import com.au.mit.vcs.common.branch.Branch;
import com.au.mit.vcs.common.commit.Commit;
import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.au.mit.vcs.common.Utility.getCurDirPath;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Created by semionn on 25.09.16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Repository.class, Repository.Cache.class, Commit.class, Branch.class})
public class RepositoryTest {
    final String vcsDirPath = ".vcs_test";

    @Rule
    public ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws IOException {
            final Path tempDirectoryPath = Files.createTempDirectory("");
            System.setProperty("user.dir", tempDirectoryPath.toAbsolutePath().toString());
        }
    };

    @Test
    public void testAddCmd() throws Exception {
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
        final Repository mockedRepository = mock(Repository.class);
        final Repository.Cache mockedCache = mock(Repository.Cache.class);
        when(mockedRepository.getCache()).thenReturn(mockedCache);
        when(mockedRepository.getTrackedDiffs()).thenReturn(new ArrayList<>());

        whenNew(Repository.class).withArguments(storagePath).thenReturn(mockedRepository);
        whenNew(Repository.Cache.class).withNoArguments().thenReturn(mockedCache);
        Repository repository = new Repository(storagePath);

        AddCmd.addFile(repository, "testfile.txt");
        verify(mockedRepository).getCache();
        verify(mockedRepository).getTrackedDiffs();
        verify(mockedCache).addFile(anyString());
    }

    @Test
    public void testMakeCommit() throws Exception {
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
        final File temp = new File(Files.createTempFile(getCurDirPath(), "test.tmp", "").toString());
        try {
            Repository repository = new Repository(storagePath);
            String commitMessage = "message";
            addFile(repository, temp, commitMessage);
            checkLog(repository, new String[]{commitMessage});
        } finally {
            Files.deleteIfExists(temp.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test
    public void testCheckoutCmd() throws Exception {
        final String branchName = "branchName";
        final Commit mockedCommit = mock(Commit.class);
        when(mockedCommit.getHash()).thenReturn("");
        final Branch mockedBranch = mock(Branch.class);
        when(mockedBranch.getLastCommit()).thenReturn(mockedCommit);

        final Repository mockedRepository = mock(Repository.class);
        when(mockedRepository.getTrackedDiffs()).thenReturn(new ArrayList<>());
        when(mockedRepository.getHead()).thenReturn(mockedCommit);
        final Map<String, Branch> branches = Collections.singletonList(branchName).stream()
                .collect(Collectors.toMap(Function.identity(), s -> mockedBranch));
        when(mockedRepository.getBranches()).thenReturn(branches);

        CheckoutCmd.checkout(mockedRepository, branchName);
        verify(mockedRepository, atLeastOnce()).getHead();
        verify(mockedRepository).getTrackedDiffs();
        verify(mockedRepository).getCommits();
        verify(mockedRepository).getBranches();
        verify(mockedRepository).setCurrentBranch(any());
        verify(mockedRepository).setHead(any());
    }

    @Test
    public void testCheckout() throws Exception {
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
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
    public void testBranchCmdMake() throws Exception {
        final String branchName = "branchName";
        final Branch mockedBranch = mock(Branch.class);
        final Commit mockedCommit = mock(Commit.class);
        when(mockedCommit.getBranch()).thenReturn(mockedBranch);

        final Repository mockedRepository = mock(Repository.class);
        when(mockedRepository.getHead()).thenReturn(mockedCommit);
        final Map<String, Branch> branches = new HashMap<>();
        when(mockedRepository.getBranches()).thenReturn(branches);

        BranchCmd.makeBranch(mockedRepository, branchName);
        verify(mockedRepository, atLeastOnce()).getBranches();
        verify(mockedRepository).getHead();
    }

    @Test
    public void testBranchCmdRemove() throws Exception {
        final String currentBranchName = "master";
        final String branchName = "branchName";
        final Branch mockedCurrentBranch = mock(Branch.class);
        when(mockedCurrentBranch.getName()).thenReturn(currentBranchName);

        final Commit mockedCurrentCommit = mock(Commit.class);
        when(mockedCurrentCommit.getBranch()).thenReturn(mockedCurrentBranch);

        final Commit mockedCommit = mock(Commit.class);
        when(mockedCommit.getPreviousCommit()).thenReturn(mockedCurrentCommit);

        final Branch mockedBranch = mock(Branch.class);
        when(mockedBranch.getName()).thenReturn(branchName);
        when(mockedBranch.possibleToDelete()).thenReturn(true);
        when(mockedBranch.getLastCommit()).thenReturn(mockedCommit);
        when(mockedCommit.getBranch()).thenReturn(mockedBranch);

        final Repository mockedRepository = mock(Repository.class);
        when(mockedRepository.getHead()).thenReturn(mockedCurrentCommit);
        when(mockedRepository.getCurrentBranch()).thenReturn(mockedCurrentBranch);
        when(mockedRepository.getCommitPath(any())).thenReturn(getCurDirPath().resolve("test"));

        final Map<String, Branch> branches = spy(new HashMap<String, Branch>(){{
            put(currentBranchName, mockedCurrentBranch);
            put(branchName, mockedBranch);
        }});
        when(mockedRepository.getBranches()).thenReturn(branches);

        BranchCmd.removeBranch(mockedRepository, branchName);
        verify(mockedRepository).getCurrentBranch();
        verify(mockedRepository, atLeastOnce()).getCommitPath(any());
        verify(mockedCommit, atLeastOnce()).getPreviousCommit();
        verify(mockedBranch, atLeastOnce()).possibleToDelete();
        verify(branches).remove(branchName);
    }

    @Test
    public void testRemoveBranch() throws Exception {
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
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
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
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
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
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
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
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
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
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
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
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
            final StatusCmd.FilesStatus filesStatus = StatusCmd.getFilesStatus(repository);
            assertEquals(1, filesStatus.getAdded().size());
            assertEquals(1, filesStatus.getModified().size());
            assertEquals(1, filesStatus.getUntracked().size());
            assertEquals(temp1.getAbsolutePath(), filesStatus.getAdded().iterator().next());
            assertEquals(temp2.getAbsolutePath(), filesStatus.getModified().iterator().next());
            assertEquals(temp3.getAbsolutePath(), filesStatus.getUntracked().iterator().next());
        } finally {
            Files.deleteIfExists(temp1.toPath());
            Files.deleteIfExists(temp2.toPath());
            Files.deleteIfExists(temp3.toPath());
            FileUtils.deleteDirectory(new File(storagePath.toString()));
        }
    }

    @Test(expected = CommandExecutionException.class)
    public void addNonexistentFile() throws IOException {
        final Path storagePath = getCurDirPath().resolve(vcsDirPath);
        Repository repository = new Repository(storagePath);
        AddCmd.addFile(repository, getCurDirPath().resolve("test").toString());
    }

    private void addFile(Repository repository, File temp, String commitMessage) throws IOException {
        String tempFileText = "text";
        try (FileOutputStream fs = new FileOutputStream(temp)) {
            fs.write(tempFileText.getBytes());
        }
        AddCmd.addFile(repository, temp.getPath());
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