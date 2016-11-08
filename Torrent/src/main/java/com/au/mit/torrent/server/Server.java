package com.au.mit.torrent.server;

/**
 * Created by Semionn on 08.11.2016.
 */
public interface Server {
    void open(int port);

    void close();
}
