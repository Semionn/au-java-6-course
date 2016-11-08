package com.au.mit.torrent;

import com.au.mit.torrent.client.ClientImpl;
import com.au.mit.torrent.tracker.SingleThreadTracker;
import com.au.mit.torrent.tracker.Tracker;
import org.junit.Test;


public class TorrentTrackerTest {

    @Test
    public void testCommunication() throws InterruptedException {
        final String hostname = "localhost";
        final int port = 8090;

        Tracker tracker = new SingleThreadTracker(hostname, port);
        Runnable clientStart = () -> new ClientImpl().connect(hostname, port);
        new Thread(tracker::start).start();

        Thread clientAThread = new Thread(clientStart, "client-A");
        clientAThread.start();
        Thread clientBThread = new Thread(clientStart, "client-B");
        clientBThread.start();

        clientAThread.join();
        clientBThread.join();
        tracker.stop();
    }
}