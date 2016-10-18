package com.au.mit.ftp.server;

import com.au.mit.ftp.common.CommandRunner;
import com.au.mit.ftp.common.exceptions.CommandExecutionException;
import com.au.mit.ftp.common.exceptions.CommunicationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.BiConsumer;

/**
 * Interact only with one client
 */
public class ClientManager {
    private final ClientDescription clientDescription;
    private final CommandRunner commandRunner;

    public ClientManager(ClientDescription clientDescription, CommandRunner commandRunner) {
        this.clientDescription = clientDescription;
        this.commandRunner = commandRunner;
    }

    public void start() {
        runSession(clientDescription.getSocket(), commandRunner::acceptCommand);
    }

    private static void runSession(Socket socket, BiConsumer<DataOutputStream, DataInputStream> consumer) {
        try (
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())
            ) {
            while (!Thread.interrupted()) {
                try {
                    consumer.accept(out, in);
                } catch (CommunicationException | CommandExecutionException e) {
                    if (e.getMessage() != null) {
                        out.writeUTF(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage(), e);
        }
    }
}
