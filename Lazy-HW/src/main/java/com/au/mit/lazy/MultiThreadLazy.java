package com.au.mit.lazy;

import java.util.function.Supplier;

/**
 * Created by semionn on 07.09.16.
 */
public class MultiThreadLazy<T> implements Lazy<T> {
    private volatile T result = null;
    private Supplier<T> supplier;
    private volatile boolean computed = false;

    public MultiThreadLazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (!computed) {
            synchronized (this) {
                if (!computed) {
                    result = supplier.get();
                    computed = true;
                }
            }
        }
        return result;
    }

    public T getResult() {
        return result;
    }

    public boolean isComputed() {
        return computed;
    }

}
