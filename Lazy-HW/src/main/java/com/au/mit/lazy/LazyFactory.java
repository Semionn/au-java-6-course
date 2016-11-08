package com.au.mit.lazy;

import java.util.function.Supplier;

/**
 * Created by semionn on 07.09.16.
 */
public class LazyFactory {

    private LazyFactory() {}

    public static <T> Lazy<T> createSingleThreadLazy(Supplier<T> supplier) {
        return new SingleThreadLazy<>(supplier);
    }

    public static <T> Lazy<T> createMultiThreadLazy(Supplier<T> supplier) {
        return new MultiThreadLazy<>(supplier);
    }

    public static <T> Lazy<T> createLockFreeLazy(Supplier<T> supplier) {
        return new LockFreeLazy<>(supplier);
    }
}
