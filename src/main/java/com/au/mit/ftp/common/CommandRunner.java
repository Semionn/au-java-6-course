package com.au.mit.ftp.common;

import com.au.mit.ftp.common.exceptions.CommandExecutionException;
import com.au.mit.ftp.common.exceptions.CommunicationException;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Allows wait commands from client and run them by theirs id and arguments
 */
public class CommandRunner {
    private final Path ftpPath;
    private final int BUFFER_SIZE = 4096;

    public CommandRunner(Path ftpPath) {
        this.ftpPath = ftpPath;
    }

    public void run(int commandId, String commandArgs, DataOutputStream out) {
        switch (commandId) {
            case 1:
                runListCmd(commandArgs, out);
                break;
            case 2:
                runGetCmd(commandArgs, out);
                break;
            default:
                throw new CommandExecutionException(String.format("Command with id '%s' not found", commandId));
        }
    }

    public void runListCmd(String commandArgs, DataOutputStream out) {
        final Path path = ftpPath.resolve(commandArgs);
        final File targetDir = new File(path.toAbsolutePath().toString());
        if (!targetDir.exists()) {
            try {
                out.writeInt(0);
                return;
            } catch (IOException e) {
                throw new CommunicationException(e.getMessage(), e);
            }
        }
        final File[] filesList = targetDir.listFiles(File::isFile);
        if (filesList != null) {
            try {
                out.writeInt(filesList.length);
                for (File file : Arrays.stream(filesList).collect(Collectors.toList())) {
                    out.writeUTF(file.getName());
                    out.writeBoolean(file.isDirectory());
                }
            } catch (IOException e) {
                throw new CommunicationException(e.getMessage(), e);
            }
        }
    }

    public void runGetCmd(String commandArgs, DataOutputStream out) {
        final Path path = ftpPath.resolve(commandArgs);
        final File targetFile = new File(path.toAbsolutePath().toString());
        try {
            if (!targetFile.exists()) {
                out.writeInt(0);
                return;
            }
            out.writeLong(targetFile.length());

            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream fileInput = new FileInputStream(targetFile)) {
                int readBytes;
                while ((readBytes = fileInput.read(buffer)) != -1 && !Thread.interrupted()) {
                    out.write(buffer, 0, readBytes);
                }
            }
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage(), e);
        }

    }

    public void acceptCommand(DataOutputStream out, DataInputStream in) {
        try {
            int commandId = in.readInt();
            run(commandId, in.readUTF(), out);
            out.flush();
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage(), e);
        }
    }
}
