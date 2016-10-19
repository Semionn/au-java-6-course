package com.au.mit.ftp.common;

import com.au.mit.ftp.common.exceptions.CommunicationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Send command to server by it's id and string of arguments
 */
public class CommandSender {
    private PrintStream commandPrintStream;

    public CommandSender(PrintStream commandPrintStream) {
        this.commandPrintStream = commandPrintStream;
    }

    private final int BUFFER_SIZE = 4096;

    public void sendListCmd(DataOutputStream out, DataInputStream in, String path) {
        try {
            out.writeInt(1);
            out.writeUTF(path);
            out.flush();
            int filesCount = in.readInt();
            for (int i = 0; i < filesCount; i++) {
                final String name = in.readUTF();
                final boolean isDir = in.readBoolean();
                commandPrintStream.println(String.format("%s: %s", isDir ? "Dir" : "File", name));
            }
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage(), e);
        }
    }

    public void sendGetCmd(DataOutputStream out, DataInputStream in, String path) {
        try {
            out.writeInt(2);
            out.writeUTF(path);
            out.flush();

            String fileName = Paths.get(path).getFileName().toString();
            File file = Paths.get(System.getProperty("user.dir"), fileName).toFile();
            Files.deleteIfExists(Paths.get(fileName));
            file.createNewFile();

            long fileLength = in.readLong();
            long readBytesCount = 0;

            byte[] buffer = new byte[BUFFER_SIZE];
            try (OutputStream fileOutput = new FileOutputStream(file)) {
                int readBytes;
                while (readBytesCount < fileLength && (readBytes = in.read(buffer)) != -1) {
                    readBytesCount += readBytes;
                    fileOutput.write(buffer, 0, readBytes);
                }
            }
            System.out.println("File successfully downloaded");
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage(), e);
        }
    }
}
