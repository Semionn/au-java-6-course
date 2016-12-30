package com.au.mit.torrent;

import com.au.mit.torrent.client.ClientImpl;
import com.au.mit.torrent.common.SmartBuffer;
import com.au.mit.torrent.common.protocol.ClientDescription;
import com.au.mit.torrent.common.protocol.FileDescription;
import com.au.mit.torrent.common.protocol.requests.tracker.ListRequest;
import com.au.mit.torrent.tracker.SingleThreadTracker;
import com.au.mit.torrent.tracker.Tracker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
            // channel reading states
            private final int READ_FILES_COUNT = 0;
            private final int READ_FILE_ID = 1;
            private final int READ_FILE_NAME = 2;
            private final int READ_FILE_SIZE = 3;

            private int currentChannelState = READ_FILES_COUNT;

            private int fileDescriptionsCount;
            private final Map<Integer, FileDescription> fileDescriptions = new HashMap<>();

            private Integer read(InvocationOnMock invocationOnMock) {
                ByteBuffer buffer = (ByteBuffer) invocationOnMock.getArguments()[0];
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
                return buffer.limit();
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
        when(mockedChannel.write(any(ByteBuffer.class))).then(mockedChannelImpl::read);

        final ClientDescription mockedClient = mock(ClientDescription.class);
        final ListRequest listRequest = new ListRequest(mockedClient);
        listRequest.handle(mockedChannel, mockedTracker);

        assertEquals(fileDescriptions.size(), mockedChannelImpl.getFileDescriptionsCount());
        assertEquals(fileDescriptions, mockedChannelImpl.getFileDescriptions());
        assertEquals(listRequest.getClient(), mockedClient);
        verify(mockedTracker).getFileDescriptions();
    }

}