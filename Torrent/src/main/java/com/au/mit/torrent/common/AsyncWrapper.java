package com.au.mit.torrent.common;

import com.au.mit.torrent.common.exceptions.AsyncRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;

import java.io.IOException;
import java.util.function.Supplier;

public class AsyncWrapper {
    private int maxCounter = 0;
    private int currentCounter = 0;

    public void resetCounter() {
        currentCounter = 0;
    }

    public <T> void wrap(Supplier<T> supplier) throws AsyncRequestNotCompleteException {
        if (currentCounter == maxCounter) {
            T res = supplier.get();
            if (res == null) {
                throw new AsyncRequestNotCompleteException();
            }
            maxCounter++;
        }
        currentCounter++;
    }

    public <T, R> void wrap(T arg, IOFunction<T, R> function) throws IOException {
        if (currentCounter == maxCounter) {
            R res = function.apply(arg);
            if (res == null) {
                throw new AsyncRequestNotCompleteException();
            }
            maxCounter++;
        }
        currentCounter++;
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

    public <R> void forloop(int start, int end, IOFunction<Integer, R>[] functions) throws IOException {
        if (maxCounter - currentCounter > (end - start) * functions.length) {
            currentCounter += end - start;
            return;
        }
        int i = (maxCounter - currentCounter) / functions.length + start;
        int j = (maxCounter - currentCounter) % functions.length;
        currentCounter = maxCounter;
        for (; i < end; i++) {
            for (; j < functions.length; j++) {
                wrap(i, functions[j]);
                currentCounter++;
                maxCounter++;
            }
            j = 0;
        }
    }

    @FunctionalInterface
    public interface IOSupplier<T> {
        T get() throws IOException;
    }

    @FunctionalInterface
    public interface IOFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
