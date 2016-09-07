package com.au.mit.lazy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by semionn on 07.09.16.
 */
public class LockFreeLazy<T> implements Lazy<T> {
    private Supplier<T> supplier;
    private volatile List<LazyResultWrapper<T>> results =
            Collections.synchronizedList(new ArrayList<LazyResultWrapper<T>>());

    public LockFreeLazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (results.size() == 0) {
            results.add(new LazyResultWrapper<>(supplier.get()));
        }
        return results.get(0).getResult();
    }
}
