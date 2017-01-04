package com.au.mit.torrent.tracker;

/**
 * Simple console for the torrent tracker
 */
public class TorrentTrackerApp {
    public static void main(String[] args) {
        try {
            Tracker tracker = new SingleThreadTracker();
            tracker.start();
        } catch (Exception e) {
            System.out.println(String.format("Exception '%s': %s", e.toString(), e.getMessage()));
        }
    }
}
