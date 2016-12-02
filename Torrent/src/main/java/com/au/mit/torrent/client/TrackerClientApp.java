package com.au.mit.torrent.client;

/**
 * Console application with ClientImpl implementation of torrent client
 */
public class TrackerClientApp {
    private static final short PORT = 8082;

    private static final String TRACKER_HOSTNAME = "localhost";
    private static final short TRACKER_PORT = 8081;

    public static void main(String[] args) {
        String publishedFile = null;
        if (args.length > 0) {
            publishedFile = args[0];
        }

        final ClientImpl client = new ClientImpl(PORT);
        client.connect(TRACKER_HOSTNAME, TRACKER_PORT);
        if (publishedFile != null) {
            client.uploadFile(publishedFile);
            client.listRequest();
        }
    }
}
