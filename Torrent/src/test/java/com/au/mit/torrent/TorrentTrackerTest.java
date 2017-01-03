package com.au.mit.torrent;

import com.au.mit.torrent.client.ClientImpl;
import com.au.mit.torrent.common.ClientAddress;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.*;
import com.au.mit.torrent.tracker.SingleThreadTracker;
import com.au.mit.torrent.tracker.Tracker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
public class TorrentTrackerTest {
    @Test
    public void testCommunication() throws InterruptedException, IOException {
        final String hostname = "localhost";
        final int port = 8081;

        final int NUMBERS_COUNT = 10000;

        Tracker tracker = new SingleThreadTracker(hostname, port);
        Thread trackerThread = new Thread(tracker::start);
        trackerThread.start();

        File fileA = File.createTempFile("testA", ".txt");
        final FileWriter fileAWriter = new FileWriter(fileA);
        for (int i = 0; i < NUMBERS_COUNT; i++) {
            fileAWriter.write(" ");
            fileAWriter.write(Integer.toString(i));
        }
        fileAWriter.close();
        fileA.deleteOnExit();

        File fileB = File.createTempFile("testB", ".txt");
        final FileWriter fileBWriter = new FileWriter(fileB);
        for (int i = 0; i < NUMBERS_COUNT; i++) {
            fileBWriter.write(" ");
            fileBWriter.write(Integer.toString(NUMBERS_COUNT-i));
        }
        fileBWriter.close();
        fileB.deleteOnExit();

        final ClientImpl clientA = new ClientImpl((short) (port + 1));
        Thread clientAThread = new Thread(() -> {
            clientA.connect(hostname, port);
            clientA.uploadFile(fileA.getAbsolutePath());
            clientA.listRequest();
        }, "client-A");
        clientAThread.start();
        clientAThread.join();

        final ClientImpl clientB = new ClientImpl((short) (port + 2));
        Thread clientBThread = new Thread(() -> {
            clientB.connect(hostname, port);
            clientB.uploadFile(fileB.getAbsolutePath());
            clientB.listRequest();
        }, "client-B");
        clientBThread.start();
        clientBThread.join();

        clientAThread = new Thread(() -> {
            clientA.downloadFile(1);
        }, "client-A");
        clientAThread.start();
        clientAThread.join();

        clientBThread = new Thread(() -> {
            clientB.downloadFile(0);
        }, "client-B");
        clientBThread.start();
        clientBThread.join();

        final File fileADownloaded = new File(fileA.getName());
        assertTrue(fileADownloaded.exists());

        final Scanner fileAScanner = new Scanner(fileADownloaded);
        for (int i = 0; i < NUMBERS_COUNT; i++) {
            assertEquals(i, fileAScanner.nextInt());
        }
        fileADownloaded.deleteOnExit();

        final File fileBDownloaded = new File(fileB.getName());
        assertTrue(fileBDownloaded.exists());

        final Scanner fileBScanner = new Scanner(fileBDownloaded);
        for (int i = 0; i < NUMBERS_COUNT; i++) {
            assertEquals(NUMBERS_COUNT - i, fileBScanner.nextInt());
        }
        fileBDownloaded.deleteOnExit();

        trackerThread.join(1000);
    }

    @Test
    public void testListRequest() throws IOException {
        final Tracker mockedTracker = mock(SingleThreadTracker.class);
        final Map<Integer, FileDescription> fileDescriptions = new HashMap<Integer, FileDescription>() {{
            final String file1Name = "file1.txt";
            final String file2Name = "file2.txt";
            final int file1Size = 13;
            final int file2Size = 42;
            put(0, new FileDescription(0, file1Name, file1Size));
            put(1, new FileDescription(1, file2Name, file2Size));
        }};
        when(mockedTracker.getFileDescriptions()).thenReturn(fileDescriptions);

        class MockedChannelImpl {
            // channel states
            private final int READ_FILES_COUNT = 0;
            private final int READ_FILE_ID = 1;
            private final int READ_FILE_NAME = 2;
            private final int READ_FILE_SIZE = 3;

            private int currentChannelState = READ_FILES_COUNT;

            private int fileDescriptionsCount;
            private final Map<Integer, FileDescription> fileDescriptions = new HashMap<>();

            private Integer write(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
                int writeCount = buffer.position();
                int id = 0;
                String name = "";
                long size = 0;
                while (buffer.hasRemaining()) {
                    switch (currentChannelState) {
                        case READ_FILES_COUNT:
                            fileDescriptionsCount = buffer.getInt();
                            break;
                        case READ_FILE_ID:
                            id = buffer.getInt();
                            break;
                        case READ_FILE_NAME:
                            name = new SmartBuffer(buffer, true).getString();
                            break;
                        case READ_FILE_SIZE:
                            size = buffer.getLong();
                            break;
                    }
                    if (currentChannelState != READ_FILE_SIZE) {
                        currentChannelState++;
                    } else {
                        currentChannelState = READ_FILE_ID;
                        fileDescriptions.put(id, new FileDescription(id, name, size));
                    }
                }
                return buffer.position() - writeCount;
            }

            int getFileDescriptionsCount() {
                return fileDescriptionsCount;
            }

            Map<Integer, FileDescription> getFileDescriptions() {
                return fileDescriptions;
            }
        }

        final SocketChannel mockedChannel = mock(SocketChannel.class);
        final MockedChannelImpl mockedChannelImpl = new MockedChannelImpl();
        when(mockedChannel.write(any(ByteBuffer.class))).then(mockedChannelImpl::write);

        final ClientDescription mockedClient = mock(ClientDescription.class);
        final ListRequest listRequest = new ListRequest(mockedClient);
        tryHandle(mockedTracker, mockedChannel, listRequest);

        assertEquals(fileDescriptions.size(), mockedChannelImpl.getFileDescriptionsCount());
        assertEquals(fileDescriptions, mockedChannelImpl.getFileDescriptions());
        assertEquals(listRequest.getClient(), mockedClient);
        verify(mockedTracker).getFileDescriptions();
    }


    @Test
    public void testSourceRequest() throws IOException {
        final Tracker mockedTracker = mock(SingleThreadTracker.class);
        Set<ClientAddress> seedAddresses = new HashSet<ClientAddress>() {{
            add(new ClientAddress("1.2.3.4", (short) 80));
            add(new ClientAddress("255.255.255.255", (short) 42));
        }};
        final Map<Integer, FileDescription> fileDescriptions = new HashMap<Integer, FileDescription>() {{
            final String file1Name = "file1.txt";
            final int file1Size = 13;
            final FileDescription fd = new FileDescription(0, file1Name, file1Size);
            put(0, fd);
            fd.setSeedsAddresses(seedAddresses);
        }};
        when(mockedTracker.getFileDescriptions()).thenReturn(fileDescriptions);

        class MockedChannelImpl {
            // channel states
            private final int WRITE_FILE_ID = 0;
            private final int READ_SEEDS_COUNT = 1;
            private final int READ_SEED_IP = 2;
            private final int READ_SEED_PORT = 3;

            private int currentChannelState = WRITE_FILE_ID;
            private final FileDescription fileDescription;
            private int seedsCount = 0;
            private Set<ClientAddress> seedAddresses = new HashSet<>();

            public MockedChannelImpl(FileDescription fileDescription) {
                this.fileDescription = fileDescription;
            }

            private Integer read(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
                int readCount = buffer.position();
                switch (currentChannelState) {
                    case WRITE_FILE_ID:
                        buffer.putInt(fileDescription.getId());
                        currentChannelState++;
                        break;
                    default:
                        buffer.putInt(-1);
                        break;
                }
                return buffer.position() - readCount;
            }

            private Integer write(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
                int writeCount = buffer.position();
                String ip = "";
                short port = 0;
                while (buffer.hasRemaining()) {
                    switch (currentChannelState) {
                        case READ_SEEDS_COUNT:
                            seedsCount = buffer.getInt();
                            break;
                        case READ_SEED_IP:
                            ip = Integer.toString(buffer.get() + 128);
                            ip += "." + Integer.toString(buffer.get() + 128);
                            ip += "." + Integer.toString(buffer.get() + 128);
                            ip += "." + Integer.toString(buffer.get() + 128);
                            break;
                        case READ_SEED_PORT:
                            port = buffer.getShort();
                            break;
                    }
                    if (currentChannelState != READ_SEED_PORT) {
                        currentChannelState++;
                    } else {
                        currentChannelState = READ_SEED_IP;
                        seedAddresses.add(new ClientAddress(ip, port));
                    }
                }
                return buffer.position() - writeCount;
            }

            Set<ClientAddress> getSeedAddresses() {
                return seedAddresses;
            }

            int getSeedsCount() {
                return seedsCount;
            }
        }

        final SocketChannel mockedChannel = mock(SocketChannel.class);
        final MockedChannelImpl mockedChannelImpl = new MockedChannelImpl(fileDescriptions.get(0));
        when(mockedChannel.write(any(ByteBuffer.class))).then(mockedChannelImpl::write);
        when(mockedChannel.read(any(ByteBuffer.class))).then(mockedChannelImpl::read);

        final ClientDescription mockedClient = mock(ClientDescription.class);
        final SourceRequest sourceRequest = new SourceRequest(mockedClient);
        tryHandle(mockedTracker, mockedChannel, sourceRequest);

        assertEquals(seedAddresses.size(), mockedChannelImpl.getSeedsCount());
        assertEquals(seedAddresses, mockedChannelImpl.getSeedAddresses());
        assertEquals(sourceRequest.getClient(), mockedClient);
        verify(mockedTracker).getFileDescriptions();
    }

    @Test
    public void testUpdateRequest() throws IOException {
        final Tracker mockedTracker = mock(SingleThreadTracker.class);
        final Map<Integer, FileDescription> fileDescriptions = new HashMap<Integer, FileDescription>() {{
            final String file1Name = "file1.txt";
            final int file1Size = 13;
            final FileDescription fd = new FileDescription(0, file1Name, file1Size);
            put(0, fd);
        }};
        when(mockedTracker.getFileDescriptions()).thenReturn(fileDescriptions);
        final boolean isUpdated = true;
        when(mockedTracker.updateSeed(any(), any())).thenReturn(isUpdated);

        class MockedChannelImpl {
            // channel states
            private final int WRITE_CLIENT_PORT = 0;
            private final int WRITE_FILES_COUNT = 1;
            private final int WRITE_FILE_ID = 2;
            private final int READ_UPDATED_CHECK = 2;

            private int currentChannelState = WRITE_CLIENT_PORT;
            private final short clientPort;
            private final List<Integer> fileIDs;
            private int fileNum = 0;

            private boolean isUpdated = false;

            public MockedChannelImpl(short clientPort, Map<Integer, FileDescription> fileDescriptions) {
                this.clientPort = clientPort;
                this.fileIDs = fileDescriptions.values().stream()
                        .map(FileDescription::getId).collect(Collectors.toList());
            }

            private Integer read(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
                int readCount = buffer.position();
                switch (currentChannelState) {
                    case WRITE_CLIENT_PORT:
                        buffer.putShort(clientPort);
                        currentChannelState++;
                        break;
                    case WRITE_FILES_COUNT:
                        buffer.putInt(fileIDs.size());
                        currentChannelState++;
                        break;
                    case WRITE_FILE_ID:
                        buffer.putInt(fileIDs.get(fileNum));
                        fileNum++;
                        break;
                    default:
                        buffer.putInt(-1);
                        break;
                }
                return buffer.position() - readCount;
            }

            private Integer write(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
                int writeCount = buffer.position();
                while (buffer.hasRemaining()) {
                    switch (currentChannelState) {
                        case READ_UPDATED_CHECK:
                            isUpdated = buffer.get() == 1;
                            break;
                    }
                }
                return buffer.position() - writeCount;
            }

            boolean isUpdated() {
                return isUpdated;
            }

            int getFileNum() {
                return fileNum;
            }
        }

        final short clientPort = (short) 8080;
        final MockedChannelImpl mockedChannelImpl = new MockedChannelImpl(clientPort, fileDescriptions);

        final SocketChannel mockedChannel = mock(SocketChannel.class);
        when(mockedChannel.write(any(ByteBuffer.class))).then(mockedChannelImpl::write);
        when(mockedChannel.read(any(ByteBuffer.class))).then(mockedChannelImpl::read);

        final ClientDescription mockedClient = mock(ClientDescription.class);
        final UpdateRequest updateRequest = new UpdateRequest(mockedClient);
        tryHandle(mockedTracker, mockedChannel, updateRequest);

        assertEquals(fileDescriptions.size(), mockedChannelImpl.getFileNum());
        assertEquals(isUpdated, mockedChannelImpl.isUpdated());
        assertEquals(updateRequest.getClient(), mockedClient);
        assertEquals(updateRequest.getClient().getLocalPort(), mockedClient.getLocalPort());
    }

    @Test
    public void testUploadRequest() throws IOException {
        final Tracker mockedTracker = mock(SingleThreadTracker.class);
        final int fileID = 42;
        when(mockedTracker.addFileDescription(any())).thenReturn(fileID);

        class MockedChannelImpl {
            // channel states
            private final int WRITE_FILE_NAME = 0;
            private final int WRITE_FILE_SIZE = 1;
            private final int READ_FILE_ID = 2;

            private int currentChannelState = WRITE_FILE_NAME;

            private final String fileName;
            private final long fileSize;
            private int fileID;

            public MockedChannelImpl(String fileName, long fileSize) {
                this.fileName = fileName;
                this.fileSize = fileSize;
            }

            private Integer read(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
                int readCount = buffer.position();
                switch (currentChannelState) {
                    case WRITE_FILE_NAME:
                        new SmartBuffer(buffer).putString(fileName);
                        currentChannelState++;
                        break;
                    case WRITE_FILE_SIZE:
                        buffer.putLong(fileSize);
                        currentChannelState++;
                        break;
                }
                return buffer.position() - readCount;
            }

            private Integer write(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
                int writeCount = buffer.position();
                fileID = 0;
                while (buffer.hasRemaining()) {
                    switch (currentChannelState) {
                        case READ_FILE_ID:
                            fileID = buffer.getInt();
                            currentChannelState++;
                            break;
                    }
                }
                return buffer.position() - writeCount;
            }

            int getFileID() {
                return fileID;
            }
        }
        final int fileSize = 13;
        final String fileName = "test1.txt";
        final MockedChannelImpl mockedChannelImpl = new MockedChannelImpl(fileName, fileSize);

        final SocketChannel mockedChannel = mock(SocketChannel.class);
        when(mockedChannel.write(any(ByteBuffer.class))).then(mockedChannelImpl::write);
        when(mockedChannel.read(any(ByteBuffer.class))).then(mockedChannelImpl::read);

        final ClientDescription mockedClient = mock(ClientDescription.class);
        final UploadRequest uploadRequest = new UploadRequest(mockedClient);
        tryHandle(mockedTracker, mockedChannel, uploadRequest);

        assertEquals(uploadRequest.getClient(), mockedClient);
        assertEquals(fileID, mockedChannelImpl.getFileID());
        verify(mockedTracker).addFileDescription(new FileDescription(fileName, fileSize));
    }

    private void tryHandle(Tracker mockedTracker, SocketChannel mockedChannel, TrackerRequest trackerRequest) throws IOException {
        int ATTEMPTS_COUNT = 5;
        int k = 0;
        boolean finished = false;
        while (k < ATTEMPTS_COUNT && !(finished = trackerRequest.handle(mockedChannel, mockedTracker))) {
            k++;
        }
        assertTrue(finished);
    }
}