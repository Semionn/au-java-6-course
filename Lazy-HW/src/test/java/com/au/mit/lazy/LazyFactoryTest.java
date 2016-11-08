package com.au.mit.lazy;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by semionn on 07.09.16.
 */
public class LazyFactoryTest {

    @Test
    public void createSingleThreadLazy() throws Exception {
        Random random = new Random();
        Lazy<Integer> lazy = LazyFactory.createSingleThreadLazy(() -> random.nextInt());
        assertTrue(Objects.equals(lazy.get(), lazy.get()));
    }

    @Test
    public void createMultiThreadLazy() throws Exception {
        final int THREAD_CNT = 4;
        Random random = new Random();
        Lazy<Integer> lazy = LazyFactory.createMultiThreadLazy(() -> random.nextInt());

        multiThreadCheck(THREAD_CNT, lazy);
    }

    private void multiThreadCheck(int THREAD_CNT, Lazy<Integer> lazy) throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(THREAD_CNT);
        List<Integer> results = Collections.synchronizedList(new ArrayList<Integer>());
        try {
            for (int i = 0; i < THREAD_CNT; i++) {
                exec.submit(() -> {
                    results.add(lazy.get());
                });
            }
        } finally {
            exec.awaitTermination(1, TimeUnit.SECONDS);
            exec.shutdown();
        }
        List<Integer> rightResults = Collections.nCopies(THREAD_CNT, results.get(0));
        assertArrayEquals(results.toArray(), rightResults.toArray());
    }

    @Test
    public void createLockFreeLazy() throws Exception {
        final int THREAD_CNT = 4;
        Random random = new Random();
        Lazy<Integer> lazy = LazyFactory.createLockFreeLazy(() -> random.nextInt());
        multiThreadCheck(THREAD_CNT, lazy);
    }

}