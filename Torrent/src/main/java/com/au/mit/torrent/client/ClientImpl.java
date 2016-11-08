package com.au.mit.torrent.client;

import com.au.mit.torrent.common.exceptions.CommunicationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Implementation of torrent tracker client
 */
public class ClientImpl implements Client {
    private final static int CONNECTION_RETRIES = 3;
    private final static int RETRY_TIME_MS = 1000;
    private final Logger logger = Logger.getLogger("ClientImpl");

    public void connect(String hostname, int port) {
        try {
            InetSocketAddress hostAddress = new InetSocketAddress(hostname, port);
            SocketChannel client = null;
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    client = SocketChannel.open(hostAddress);
                } catch (IOException e) {
                    logger.warning(String.format("Attempt %d: Connection opening failed due to error: %s", i, e.getMessage()));
                    Thread.sleep(RETRY_TIME_MS);
                }
            }
            if (client == null) {
                throw new CommunicationException(String.format("Connection refused after %d attempts", CONNECTION_RETRIES));
            }

            logger.info("Client started");

            String threadName = Thread.currentThread().getName();

            String[] messages = new String[]
                    {threadName + ": test1", threadName + ": test2", threadName + ": test3"};

            for (int i = 0; i < messages.length; i++) {
                byte[] message = messages[i].getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(message);
                client.write(buffer);
                logger.info(String.format("Sent message '%s'", messages[i]));
                buffer.clear();
                Thread.sleep(1000);
            }
            client.close();
        } catch (IOException e) {
            throw new CommunicationException(e);
        } catch (InterruptedException ignored) { }
    }
}
