package com.au.mit.torrent.common;

import com.au.mit.torrent.common.exceptions.AsyncReadRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.AsyncWriteRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;

import java.io.IOException;
import java.util.function.Supplier;

public class AsyncWrapper {
    private int maxCounter = 0;
    private int currentCounter = 0;

    public void resetCounter() {
        currentCounter = 0;
    }

    public <T> void wrapRead(Supplier<T> supplier) throws AsyncReadRequestNotCompleteException {
        if (currentCounter == maxCounter) {
            T res = supplier.get();
            if (res == null) {
                throw new AsyncReadRequestNotCompleteException();
            }
            maxCounter++;
        }
        currentCounter++;
    }

    public <T> void wrapWrite(Supplier<T> supplier) throws AsyncWriteRequestNotCompleteException {
        if (currentCounter == maxCounter) {
            T res = supplier.get();
            if (res == null) {
                throw new AsyncWriteRequestNotCompleteException();
            }
            maxCounter++;
        }
        currentCounter++;
    }

    public <T, R> void wrapWrite(T arg, IOFunction<T, R> function) throws IOException {
        if (currentCounter == maxCounter) {
            R res = function.apply(arg);
            if (res == null) {
                throw new AsyncWriteRequestNotCompleteException();
            }
            maxCounter++;
        }
        currentCounter++;
    }

    public <T, R> void wrapRead(T arg, IOFunction<T, R> function) throws IOException {
        if (currentCounter == maxCounter) {
            R res = function.apply(arg);
            if (res == null) {
                throw new AsyncReadRequestNotCompleteException();
            }
            maxCounter++;
        }
        currentCounter++;
    }

    public void channelInteract(IOSupplier<Integer> supplier) throws IOException {
        if (currentCounter == maxCounter) {
            Integer res = supplier.get();
            if (res == null) {
                throw new AsyncReadRequestNotCompleteException();
            }
            if (res == -1) {
                throw new EmptyChannelException();
            }
            maxCounter++;
        }
        currentCounter++;
    }

    public <R> void forloopWrite(int start, int end, IOFunction<Integer, R>[] functions) throws IOException {
        forloop(start, end, functions, this::wrapWrite);
    }

    public <R> void forloopRead(int start, int end, IOFunction<Integer, R>[] functions) throws IOException {
        forloop(start, end, functions, this::wrapRead);
    }

    private <R> void forloop(int start, int end, IOFunction<Integer, R>[] functions,
                             IOBiSupplier<Integer, IOFunction<Integer, R>> wrapFunction) throws IOException {
        if (maxCounter - currentCounter > (end - start) * functions.length) {
            currentCounter += end - start;
            return;
        }
        int i = (maxCounter - currentCounter) / functions.length + start;
        int j = (maxCounter - currentCounter) % functions.length;
        currentCounter = maxCounter;
        for (; i < end; i++) {
            for (; j < functions.length; j++) {
                wrapFunction.apply(i, functions[j]);
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

    @FunctionalInterface
    private interface IOBiSupplier<T1, T2> {
        void apply(T1 t1, T2 t2) throws IOException;
    }
}
