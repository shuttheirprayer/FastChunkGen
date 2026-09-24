package com.misanthropy.fastchunkgen.base.common.scheduler;

import com.google.common.base.Preconditions;
import com.misanthropy.fastchunkgen.base.common.GlobalExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class NeighborLockingTask<T> implements ScheduledTask {

    private static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Neighbor Locking");

    private final SchedulingManager schedulingManager;
    private final long center;
    private final long[] names;
    private final Supplier<CompletableFuture<T>> action;
    private final String desc;
    private final boolean async;
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private boolean acquired = false;

    public NeighborLockingTask(SchedulingManager schedulingManager, long center, long[] names, Supplier<CompletableFuture<T>> action, String desc, boolean async) {
        this.schedulingManager = schedulingManager;
        this.center = center;
        this.names = names;
        this.action = action;
        this.desc = desc;
        this.async = async;

        this.schedulingManager.enqueue(this);
    }


    @Override
    public boolean tryPrepare() {
        final NeighborLockingManager lockingManager = this.schedulingManager.getNeighborLockingManager();
        for (long l : names) {
            if (lockingManager.isLocked(l)) {
                lockingManager.addReleaseListener(l, () -> this.schedulingManager.enqueue(this));
                return false;
            }
        }
        int acquiredCount = 0;
        try {
            for (long l : names) {
                lockingManager.acquireLock(l);
                acquiredCount++;
            }
        } catch (Throwable t) {
            for (int i = 0; i < acquiredCount; i++) {
                if (lockingManager.isLocked(names[i])) lockingManager.releaseLock(names[i]);
            }
            throw t;
        }
        acquired = true;
        return true;
    }

    private void releaseLocks() {
        this.schedulingManager.getExecutor().execute(() -> {
            final NeighborLockingManager lockingManager = this.schedulingManager.getNeighborLockingManager();
            for (long l : names) {
                if (lockingManager.isLocked(l)) lockingManager.releaseLock(l);
            }
        });
    }

    @Override
    public void runTask(Runnable postAction) {
        Preconditions.checkNotNull(postAction);
        if (!acquired) throw new IllegalStateException();
        final CompletableFuture<T> future;
        try {
            future = Preconditions.checkNotNull(this.action.get(), "future");
        } catch (Throwable t) {
            this.acquired = false;
            releaseLocks();
            this.future.completeExceptionally(t);
            throw t;
        }
        future.handleAsync((result, throwable) -> {
            this.acquired = false;
            releaseLocks();
            try {
                postAction.run();
            } catch (Throwable t) {
                LOGGER.error("Error while running chunk task post action for {}", this.desc, t);
            }
            if (throwable != null) this.future.completeExceptionally(throwable);
            else this.future.complete(result);
            return null;
        }, GlobalExecutors.invokingExecutor);
    }

    @Override
    public long centerPos() {
        return center;
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    public CompletableFuture<T> getFuture() {
        return future;
    }
}
