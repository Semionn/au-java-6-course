package com.au.mit.torrent.tracker;

public class TorrentTrackerApp {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8081;

    public static void main(String[] args) {
        Tracker tracker = new SingleThreadTracker(HOSTNAME, PORT);
        tracker.start();
    }
}
