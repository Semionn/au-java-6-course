package com.au.mit.torrent;

import com.au.mit.torrent.client.ClientImpl;
import com.au.mit.torrent.tracker.SingleThreadTracker;
import com.au.mit.torrent.tracker.Tracker;
import org.junit.Test;


public class TorrentTrackerTest {

    @Test
    public void testCommunication() throws InterruptedException {
        final String hostname = "localhost";
        final int port = 8081;

        Tracker tracker = new SingleThreadTracker(hostname, port);
        Thread trackerThread = new Thread(tracker::start);
        trackerThread.start();

        Thread clientAThread = new Thread(() -> new ClientImpl(port + 1).connect(hostname, port), "client-A");
        clientAThread.start();
        Thread clientBThread = new Thread(() -> new ClientImpl(port + 2).connect(hostname, port), "client-B");
        clientBThread.start();

        clientAThread.join();
        clientBThread.join();
        trackerThread.join(2000);
    }
}