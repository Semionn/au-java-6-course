package com.au.mit.ftp.client;

import java.util.Arrays;
import java.util.Scanner;

/**
 * Console application for ftp client
 * Allows run commands of Client class
 */
public class ClientApp {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Not enough arguments - provide ftp server address and port number");
            return;
        }

        String address = args[1];
        int port = Integer.parseInt(args[1]);

        Client client = new Client(port, address);
        client.connect();

        System.out.println(String.format("Client connected to ftp server at %s:%s", address, port));

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            processCommand(client, input);
        }
    }

    private static void processCommand(Client client, String input) {
        final String[] splitted = input.split(" ");
        String command = splitted[0];
        final String[] arguments = Arrays.copyOfRange(splitted, 1, splitted.length);
        switch (command) {
            case "disconnect":
                client.disconnect();
                break;
            case "connect":
                client.connect();
                break;
            case "list":
                if (arguments.length < 1) {
                    System.out.println("Not enough arguments - provide path");
                    break;
                }
                client.executeGet(arguments[0]);
                break;
            case "get":
                if (arguments.length < 1) {
                    System.out.println("Not enough arguments - provide path");
                    break;
                }
                client.executeGet(arguments[0]);
                break;
            default:
                System.out.println("Unknown command");
                break;
        }
    }
}
