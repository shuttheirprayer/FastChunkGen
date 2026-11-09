package com.misanthropy.fastchunkgen.base.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CFUtil {

    private static final long PARK_NANOS = 100_000_000L;
    private static final AtomicBoolean NESTED_FUTURE_REPORTED = new AtomicBoolean();

    public static <V> CompletableFuture<List<V>> unwrapNestedFutures(List<V> values) {
        for (int i = 0, size = values.size(); i < size; i++) {
            if (values.get(i) instanceof CompletionStage<?>) return unwrapNestedFuturesSlow(values);
        }
        return CompletableFuture.completedFuture(values);
    }

    @SuppressWarnings("unchecked")
    private static <V> CompletableFuture<List<V>> unwrapNestedFuturesSlow(List<V> values) {
        final List<Object> unwrapped = new ArrayList<>(values);
        final List<CompletableFuture<?>> pending = new ArrayList<>();
        for (int i = 0; i < unwrapped.size(); i++) {
            if (unwrapped.get(i) instanceof CompletionStage<?> stage) {
                if (NESTED_FUTURE_REPORTED.compareAndSet(false, true)) {
                    Log.MAIN.warn("A chunk future completed with another future ({}) instead of its result, which crashes vanilla chunk loading (issue #30). FastChunkGen is waiting for the inner future instead. Please report this line together with your mod list.", stage);
                }
                final int index = i;
                pending.add(stage.toCompletableFuture().thenAccept(value -> unwrapped.set(index, value)));
            }
        }
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                .thenCompose(unused -> unwrapNestedFutures((List<V>) unwrapped));
    }

    public static <T> T join(CompletableFuture<T> future) {
        await(future, -1L);
        return future.join();
    }

    public static boolean await(CompletableFuture<?> future, long timeoutNanos) {
        if (future.isDone()) return true;
        final FutureBlocker blocker = new FutureBlocker(future, timeoutNanos);
        try {
            ForkJoinPool.managedBlock(blocker);
        } catch (InterruptedException e) {
            blocker.interrupted = true;
        } finally {
            if (blocker.interrupted) Thread.currentThread().interrupt();
        }
        return future.isDone();
    }

    private static final class FutureBlocker implements ForkJoinPool.ManagedBlocker {

        private final CompletableFuture<?> future;
        private final boolean bounded;
        private final long deadline;
        private boolean interrupted = false;

        private FutureBlocker(CompletableFuture<?> future, long timeoutNanos) {
            this.future = future;
            this.bounded = timeoutNanos >= 0L;
            this.deadline = this.bounded ? System.nanoTime() + timeoutNanos : 0L;
        }

        @Override
        public boolean block() {
            long park = PARK_NANOS;
            if (this.bounded) {
                final long remaining = this.deadline - System.nanoTime();
                if (remaining <= 0L) return true;
                park = Math.min(park, remaining);
            }
            try {
                this.future.get(park, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                this.interrupted = true;
            } catch (Throwable ignored) {
            }
            return isReleasable();
        }

        @Override
        public boolean isReleasable() {
            return this.future.isDone() || (this.bounded && this.deadline - System.nanoTime() <= 0L);
        }
    }

}
