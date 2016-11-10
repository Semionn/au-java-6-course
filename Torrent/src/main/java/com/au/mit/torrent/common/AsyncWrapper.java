package com.au.mit.torrent.common;

import com.au.mit.torrent.common.exceptions.AsyncRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Created by semionn on 10.11.16.
 */
public class AsyncWrapper {
    private int maxCounter = 0;
    private int currentCounter = 0;

    public void resetCounter() {
        currentCounter = 0;
    }

    public <T> void wrap(Supplier<T> supplier) {
        if (currentCounter == maxCounter) {
            T res = supplier.get();
            if (res == null) {
                return;
            }
            maxCounter++;
        }
        currentCounter++;
    }

    public <T> T wrap(T obj, Supplier<T> supplier) throws AsyncRequestNotCompleteException {
        if (currentCounter == maxCounter) {
            if (obj == null) {
                obj = supplier.get();
                if (obj == null) {
                    throw new AsyncRequestNotCompleteException();
                }
            }
            maxCounter++;
        }
        currentCounter++;
        return obj;
    }

    public void channelInteract(IOSupplier<Integer> supplier) throws IOException {
        if (currentCounter == maxCounter) {
            Integer res = supplier.get();
            if (res == null) {
                throw new AsyncRequestNotCompleteException();
            }
            if (res == -1) {
                throw new EmptyChannelException();
            }
            maxCounter++;
        }
        currentCounter++;
    }

    @FunctionalInterface
    public interface IOSupplier<T> {
        T get() throws IOException;
    }

}
