package com.misanthropy.fastchunkgen.base.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.misanthropy.fastchunkgen.base.ModuleEntryPoint;
import com.misanthropy.fastchunkgen.base.common.util.C2MEForkJoinWorkerThreadFactory;

import java.util.concurrent.*;

public class GlobalExecutors {

    private static final C2MEForkJoinWorkerThreadFactory factory = new C2MEForkJoinWorkerThreadFactory("fastchunkgen", "FastChunkGen worker #%d", Integer.getInteger("fastchunkgen.workerPriority", Thread.MIN_PRIORITY));
    public static final int GLOBAL_EXECUTOR_PARALLELISM = (int) ModuleEntryPoint.globalExecutorParallelism;
    public static final ForkJoinPool executor = new ForkJoinPool(
            GLOBAL_EXECUTOR_PARALLELISM,
            factory,
            null,
            true
    );

    public static final Executor invokingExecutor = r -> {
        if (Thread.currentThread().getThreadGroup() == factory.getThreadGroup()) {
            r.run();
        } else {
            executor.execute(r);
        }
    };

    public static final ExecutorService asyncScheduler = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fcg_sched").build());


}
