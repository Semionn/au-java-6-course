package com.au.mit.lazy;
import java.util.function.Supplier;

/**
 * Created by semionn on 07.09.16.
 */
public class SingleThreadLazy<T> implements Lazy<T> {
    private LazyResultWrapper<T> wrapper = new NoLazyResult<>();
    private Supplier<T> supplier;

    public SingleThreadLazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (wrapper == null) {
            wrapper = new LazyResultWrapper<>(supplier.get());
        }
        return wrapper.getResult();
    }
}
