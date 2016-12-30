package com.au.mit.torrent.common;

import com.au.mit.torrent.common.exceptions.AsyncReadRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.AsyncWriteRequestNotCompleteException;
import com.au.mit.torrent.common.exceptions.EmptyChannelException;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Wrapper for non-blocking nio channels.
 * Allows to use nio channels read-write operations without checking the buffer filling
 */
public class AsyncWrapper {
    private int maxCounter = 0;
    private int currentCounter = 0;

    /**
     * Should be called at first in every method, which use wrapping
     */
    public void resetCounter() {
        currentCounter = 0;
    }

    /**
     * Wraps supplier from channels processing method
     * @param supplier supplier, which may stay incomplete after execution. In this case, it should return null,
     *                 not null otherwise. SmartBuffer reading methods would be suitable
     * @param <T> return type of the supplier
     * @throws AsyncReadRequestNotCompleteException throws if supplier returns null
     */
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

    /**
     * Wraps supplier from channels processing method
     * @param supplier supplier, which may stay incomplete after execution. In this case, it should return null,
     *                 not null otherwise. SmartBuffer reading methods would be suitable
     * @param <T> return type of the supplier
     * @throws AsyncWriteRequestNotCompleteException throws if supplier returns null
     */
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

    /**
     * Wraps supplier from channels processing method
     * @param arg argument for the function
     * @param function function, which may stay incomplete after execution. In this case, it should return null,
     *                 not null otherwise. SmartBuffer reading methods would be suitable
     * @param <T> type of function argument
     * @param <R> return type of the function
     * @throws AsyncWriteRequestNotCompleteException throws if supplier returns null
     * @throws IOException rethrows function IOExceptions
     */
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

    /**
     * Wraps supplier from channels processing method
     * @param arg argument for the function
     * @param function function, which may stay incomplete after execution. In this case, it should return null,
     *                 not null otherwise. SmartBuffer reading methods would be suitable
     * @param <T> type of function argument
     * @param <R> return type of the function
     * @throws AsyncReadRequestNotCompleteException throws if supplier returns null
     * @throws IOException rethrows function IOExceptions
     */
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

    /**
     * Wraps supplier for channel interaction, which should return null if the operation still incomplete,
     * -1 if the nio channel is empty and any other value in the rest cases
     * @param supplier supplier with channel interaction
     * @throws AsyncReadRequestNotCompleteException if supplier returns null
     * @throws EmptyChannelException if supplier returns -1
     * @throws IOException if supplier throws IOException
     */
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

    /**
     * Wraps forloop iteration from the channel processing method
     * @param start initial value for loop variable (inclusive)
     * @param end finite value for loop variable (exclusive)
     * @param functions array of forloop body functions. They should receive forloop variable as argument
     * @param <R> return type of the functions
     * @throws AsyncWriteRequestNotCompleteException if function returns null
     * @throws IOException if one of the functions throws it
     */
    public <R> void forloopWrite(int start, int end, IOFunction<Integer, R>[] functions) throws IOException {
        forloop(start, end, functions, this::wrapWrite);
    }

    /**
     * Wraps forloop iteration from the channel processing method
     * @param start initial value for loop variable (inclusive)
     * @param end finite value for loop variable (exclusive)
     * @param functions array of forloop body functions. They should receive forloop variable as argument
     * @param <R> return type of the functions
     * @throws IOException if one of the functions throws it
     * @throws AsyncReadRequestNotCompleteException if function returns null
     */
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
