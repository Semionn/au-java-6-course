package com.au.mit.lazy;

/**
 * Created by semionn on 07.09.16.
 */
class LazyResultWrapper<T> {
    private T result;

    LazyResultWrapper(T result) {
        this.result = result;
    }

    T getResult() {
        return result;
    }
}

class NoLazyResult<T> extends LazyResultWrapper<T> {
    NoLazyResult() {
        super(null);
    }
}