package com.au.mit.ftp.server;

import com.au.mit.ftp.client.Client;
import com.au.mit.ftp.common.exceptions.CommunicationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class ServerTest {
    private final int serverPortNumber = 33094;
    private final int connectionRetriesCount = 10;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testList() throws InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);

        Server server = new Server(serverPortNumber);
        Client client = new Client(serverPortNumber, "localhost", printStream);
        server.start();
        for (int i = 0; i < connectionRetriesCount; i++) {
            try {
                client.connect();
                break;
            } catch (CommunicationException ignored) {
                Thread.sleep(100);
            }
        }
        assertTrue(client.isConnected());
        client.executeList("");
        String command1Result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        baos.reset();
        client.executeList("");
        String command2Result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(command1Result, command2Result);
        client.disconnect();
        server.stop();
    }

    @Test
    public void testGet() throws Exception {
        final String tempFileName = "temp.txt";
        final String fileContent = "Hello world!";
        temporaryFolder.create();
        final File tempFile = temporaryFolder.newFile(tempFileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
        bw.write(fileContent);
        bw.close();

        Server server = new Server(serverPortNumber);
        Client client = new Client(serverPortNumber, "localhost");
        server.start();
        for (int i = 0; i < connectionRetriesCount; i++) {
            try {
                client.connect();
                break;
            } catch (CommunicationException ignored) {
                Thread.sleep(100);
            }
        }
        assertTrue(client.isConnected());
        client.executeList("");
        client.executeGet(tempFile.getAbsolutePath());
        assertEquals(Arrays.asList(fileContent), Files.readAllLines(Paths.get(tempFileName)));
        client.disconnect();
        server.stop();
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException, BrokenBarrierException, TimeoutException {
        Server server = new Server(serverPortNumber);
        server.start();
        final int clientCount = 10;
        final CyclicBarrier barrier = new CyclicBarrier(clientCount + 1);
        List<Thread> clientThreads = new ArrayList<>();

        for (int j = 0; j < clientCount; j++) {
            clientThreads.add(new Thread(() -> {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream printStream = new PrintStream(baos);
                    Client client = new Client(serverPortNumber, "localhost", printStream);
                    while (true) {
                        try {
                            client.connect();
                            break;
                        } catch (CommunicationException ignored) {
                            Thread.sleep(100);
                        }
                    }
                    assertTrue(client.isConnected());
                    client.executeList("");
                    String command1Result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    baos.reset();
                    client.executeList("");
                    String command2Result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    assertEquals(command1Result, command2Result);
                    client.disconnect();
                    barrier.await();
                } catch (InterruptedException ignored) {}
                catch (BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }));
            clientThreads.get(j).setDaemon(true);
        }

        clientThreads.forEach(Thread::start);
        barrier.await(5, TimeUnit.SECONDS);
        assertEquals(0, barrier.getNumberWaiting());
        server.stop();
    }
}