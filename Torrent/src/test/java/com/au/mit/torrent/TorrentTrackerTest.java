package com.au.mit.torrent;

import com.au.mit.torrent.client.ClientImpl;
import com.au.mit.torrent.tracker.SingleThreadTracker;
import com.au.mit.torrent.tracker.Tracker;
import org.junit.Test;

import java.io.*;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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
}