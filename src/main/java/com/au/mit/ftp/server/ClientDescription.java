package com.au.mit.ftp.server;

import java.net.Socket;

/**
 * Description of client and session for interaction with him
 */
public class ClientDescription {
    private Socket socket;

    public ClientDescription(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }
}
