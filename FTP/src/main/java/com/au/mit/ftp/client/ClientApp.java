package com.au.mit.ftp.client;

import com.au.mit.ftp.common.ClientCommandType;
import com.au.mit.ftp.common.ServerCommandType;

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

        String address = args[0];
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
        String commandName = splitted[0];
        final String[] arguments = Arrays.copyOfRange(splitted, 1, splitted.length);

        ClientCommandType clientCommand = ClientCommandType.getByName(commandName);
        if (clientCommand != null) {
            switch (clientCommand) {
                case DISCONNECT:
                    client.disconnect();
                    break;
                case CONNECT:
                    client.connect();
                    break;
            }
        } else {
            ServerCommandType serverCommand = ServerCommandType.getByName(commandName);
            if (serverCommand != null) {
                switch (serverCommand) {
                    case LIST:
                        String listPath = "";
                        if (arguments.length >= 1) {
                            listPath = arguments[0];
                        }
                        client.executeList(listPath);
                        break;
                    case GET:
                        String getPath = "";
                        if (arguments.length >= 1) {
                            getPath = arguments[0];
                        }
                        client.executeGet(getPath);
                        break;
                }
            } else {
                System.out.println("Unknown command");
            }
        }
    }
}
