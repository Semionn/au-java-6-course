package com.au.mit.torrent.tracker;

public class TorrentTrackerApp {
    public static void main(String[] args) {
        Tracker tracker = new SingleThreadTracker();
        tracker.start();
    }
}
