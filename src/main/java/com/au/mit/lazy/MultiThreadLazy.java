package com.au.mit.lazy;

import java.util.function.Supplier;

/**
 * Created by semionn on 07.09.16.
 */
public class MultiThreadLazy<T> implements Lazy<T> {
    private volatile LazyResultWrapper<T> wrapper = new NoLazyResult<>();
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
                    wrapper = new LazyResultWrapper<>(supplier.get());
                    computed = true;
                }
            }
        }
        return wrapper.getResult();
    }

    public LazyResultWrapper<T> getWrapper() {
        return wrapper;
    }

    public boolean isComputed() {
        return computed;
    }

}
