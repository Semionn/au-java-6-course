package com.au.mit.ftp.server;

import java.util.Scanner;

/**
 * Console application for ftp server
 * Correct stopping of server implies typing command "exit"
 */
public class ServerApp {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Not enough arguments - provide port number");
            return;
        }

        int port = Integer.parseInt(args[0]);

        Server server = new Server(port);
        server.start();

        System.out.println(String.format("Server started in port %s", port));

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            if (scanner.nextLine().equals("exit")) {
                server.stop();
                break;
            } else {
                System.out.println("Unknown command - use 'exit' to stop server");
            }
        }
    }
}
