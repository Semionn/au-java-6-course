package com.au.mit.torrent;

import com.au.mit.torrent.client.ClientImpl;
import com.au.mit.torrent.tracker.SingleThreadTracker;
import com.au.mit.torrent.tracker.Tracker;
import org.junit.Test;

import java.io.File;
import java.io.IOException;


public class TorrentTrackerTest {

    @Test
    public void testCommunication() throws InterruptedException, IOException {
        final String hostname = "localhost";
        final int port = 8081;

        Tracker tracker = new SingleThreadTracker(hostname, port);
        Thread trackerThread = new Thread(tracker::start);
        trackerThread.start();

        File fileA = File.createTempFile("testA", "txt");
        fileA.deleteOnExit();

        File fileB = File.createTempFile("testB", "txt");
        fileB.deleteOnExit();

        final ClientImpl clientA = new ClientImpl(port + 1);
        Thread clientAThread = new Thread(() -> {
            clientA.connect(hostname, port);
            clientA.uploadFile(fileA.getAbsolutePath());
            clientA.updateTrackerFiles();
        }, "client-A");
        clientAThread.start();

        final ClientImpl clientB = new ClientImpl(port + 2);
        Thread clientBThread = new Thread(() -> {
            clientB.connect(hostname, port);
            clientB.uploadFile(fileB.getAbsolutePath());
            clientB.updateTrackerFiles();
        }, "client-B");
        clientBThread.start();

        clientAThread.join();
        clientBThread.join();
        trackerThread.join(2000);
    }
}