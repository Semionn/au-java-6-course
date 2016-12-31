package com.au.mit.ftp.server;

import com.au.mit.ftp.common.CommandRunner;
import com.au.mit.ftp.common.exceptions.CommunicationException;
import com.au.mit.ftp.common.exceptions.InternalServerException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Primary class of ftp server
 * It accepts new clients and transfer them to separate ClientManager
 */
public class Server {
    private int portNumber;
    private final ExecutorService threadPool;
    private Thread serverThread;
    private final CommandRunner commandRunner;
    private ServerSocket serverSocket;

    public Server(int portNumber) {
        this(portNumber, 4);
    }

    public Server(int portNumber, int threadCount) {
        this.portNumber = portNumber;
        threadPool = Executors.newFixedThreadPool(threadCount);
        commandRunner = new CommandRunner(Paths.get(System.getProperty("user.dir")));
    }

    public void start() {
        if (serverThread != null) {
            stop();
        }
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(portNumber);
                while (!Thread.interrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        final ClientManager clientManager = new ClientManager(new ClientDescription(clientSocket), commandRunner);
                        threadPool.execute(clientManager::start);
                    } catch (CommunicationException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        if (!e.getMessage().equals("Socket closed")) {
                            e.printStackTrace();
                        }
                    }
                }
                serverSocket.close();
            } catch (IOException e) {
                throw new CommunicationException(e.getMessage(), e);
            }
        });
        serverThread.start();
    }

    public void stop() {
        serverThread.interrupt();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new InternalServerException(e.getMessage(), e);
            }
        }
        try {
            serverThread.join();
        } catch (InterruptedException ignored) {
        }
    }
}
