package com.au.mit.lazy;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Created by semionn on 07.09.16.
 */
public class LockFreeLazy<T> implements Lazy<T> {
    private Supplier<T> supplier;
    private volatile AtomicReference<LazyResultWrapper<T>> wrapper = new AtomicReference<>();

    public LockFreeLazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        wrapper.compareAndSet(null, new LazyResultWrapper<>(supplier.get()));
        return wrapper.get().getResult();
    }
}
