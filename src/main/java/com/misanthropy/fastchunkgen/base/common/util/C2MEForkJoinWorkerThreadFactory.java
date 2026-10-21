package com.misanthropy.fastchunkgen.base.common.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class C2MEForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final AtomicLong serial = new AtomicLong(0);
    private final String groupName;
    private final String namePattern;
    private final int priority;

    private final ExecutorService threadCreator;
    private final ThreadGroup threadGroup;

    public C2MEForkJoinWorkerThreadFactory(String groupName, String namePattern, int priority) {
        this.groupName = groupName;
        this.namePattern = namePattern;
        this.priority = priority;

        this.threadGroup = new ThreadGroup(this.groupName);
        this.threadCreator = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(String.format("%s daemon", this.groupName))
                        .setPriority(Thread.NORM_PRIORITY - 1)
                        .setDaemon(true)
                        .setThreadFactory(r -> new Thread(this.threadGroup, r))
                        .build()
        );
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        final CompletableFuture<C2MEForkJoinWorkerThread> future = CompletableFuture.supplyAsync(() -> {
            final C2MEForkJoinWorkerThread newThread = new C2MEForkJoinWorkerThread(pool);
            newThread.setName(String.format(namePattern, serial.incrementAndGet()));
            newThread.setPriority(priority);
            newThread.setDaemon(true);
            return newThread;
        }, threadCreator);
        boolean interrupted = false;
        while (!future.isDone()) {
            LockSupport.parkNanos("Waiting for worker thread creation", 100_000L);
            if (Thread.interrupted()) interrupted = true;
        }
        if (interrupted) Thread.currentThread().interrupt();
        return future.join();
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public static class C2MEForkJoinWorkerThread extends ForkJoinWorkerThread {

        public Object splinePoint;
        public final double[][] scratch = new double[16][];
        public int scratchDepth;

        protected C2MEForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }

    }
}
