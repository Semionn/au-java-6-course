package com.au.mit.ftp.client;

import com.au.mit.ftp.common.CommandSender;
import com.au.mit.ftp.common.exceptions.CommunicationException;
import com.au.mit.ftp.common.exceptions.DisconnectedException;

import java.io.*;
import java.net.Socket;
import java.util.function.BiFunction;

/**
 * Primary client class for ftp
 */
public class Client {
    private final int portNumber;
    private final String hostName;
    private final CommandSender commandSender;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public Client(int portNumber, String hostName, PrintStream printStream) {
        this.portNumber = portNumber;
        this.hostName = hostName;
        commandSender = new CommandSender(printStream);
    }

    public Client(int portNumber, String hostName) {
        this(portNumber, hostName, System.out);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public void connect() {
        try {
            socket = new Socket(hostName, portNumber);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            disconnect();
            throw new CommunicationException(e.getMessage(), e);
        }
    }

    public void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void executeList(String path) {
        checkConnection();
        commandSender.sendListCmd(out, in, path);
    }

    public void executeGet(String path) {
        checkConnection();
        commandSender.sendGetCmd(out, in, path);
    }

    private void checkConnection() {
        if (!isConnected()) {
            throw new DisconnectedException("Client is disconnected");
        }
    }
}
