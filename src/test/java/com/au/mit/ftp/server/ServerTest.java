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
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by semionn on 11.10.16.
 */
public class ServerTest {
    final int serverPortNumber = 33094;
    final int connectionRetriesCount = 10;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testList() throws Exception {
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
}