package com.misanthropy.fastchunkgen.threading.worldgen.common;

import com.google.common.base.Preconditions;
import com.ibm.asyncutil.locks.AsyncLock;
import com.misanthropy.fastchunkgen.base.common.GlobalExecutors;
import com.misanthropy.fastchunkgen.base.common.scheduler.NeighborLockingTask;
import com.misanthropy.fastchunkgen.base.common.scheduler.SchedulingManager;
import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import static com.misanthropy.fastchunkgen.threading.worldgen.common.ChunkStatusUtils.ChunkStatusThreadingType.AS_IS;
import static com.misanthropy.fastchunkgen.threading.worldgen.common.ChunkStatusUtils.ChunkStatusThreadingType.PARALLELIZED;
import static com.misanthropy.fastchunkgen.threading.worldgen.common.ChunkStatusUtils.ChunkStatusThreadingType.SINGLE_THREADED;

public class ChunkStatusUtils {

    public static ChunkStatusThreadingType getThreadingType(final ChunkStatus status) {
        if (status.equals(ChunkStatus.STRUCTURE_STARTS)
                || status.equals(ChunkStatus.STRUCTURE_REFERENCES)
                || status.equals(ChunkStatus.BIOMES)
                || status.equals(ChunkStatus.NOISE)
                || status.equals(ChunkStatus.SPAWN)
                || status.equals(ChunkStatus.SURFACE)
                || status.equals(ChunkStatus.CARVERS)) {
            return PARALLELIZED;
        } else if (status.equals(ChunkStatus.FEATURES)) {
            return Config.allowThreadedFeatures ? PARALLELIZED : SINGLE_THREADED;
        }
        return AS_IS;
    }

    public static <T> CompletableFuture<T> runChunkGenWithLock(ChunkPos target, ChunkStatus status, int radius, SchedulingManager schedulingManager, boolean async, Supplier<CompletableFuture<T>> action) {
        Preconditions.checkNotNull(status);
        final int diameter = 2 * radius + 1;
        final long[] lockTargets = new long[diameter * diameter];
        int index = 0;
        for (int x = target.x - radius; x <= target.x + radius; x++)
            for (int z = target.z - radius; z <= target.z + radius; z++)
                lockTargets[index++] = ChunkPos.asLong(x, z);

        final NeighborLockingTask<T> task = new NeighborLockingTask<>(
                schedulingManager,
                target.toLong(),
                lockTargets,
                action,
                target + " " + status,
                async
        );
        return task.getFuture();
    }

    public static boolean isCancelled(ChunkHolder holder, ChunkStatus targetStatus) {
        if (!Config.allowGenerationCancellation) return false;
        return ChunkLevel.generationStatus(holder.getTicketLevel()).getIndex() < targetStatus.getIndex();
    }

    public enum ChunkStatusThreadingType {

        PARALLELIZED() {
            @Override
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture) {
                return CompletableFuture.supplyAsync(completableFuture, GlobalExecutors.executor).thenCompose(Function.identity());
            }
        },
        SINGLE_THREADED() {
            @Override
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture) {
                Preconditions.checkNotNull(lock);
                return lock.acquireLock().toCompletableFuture().thenComposeAsync(lockToken -> {
                    try {
                        return completableFuture.get();
                    } finally {
                        lockToken.releaseLock();
                    }
                }, GlobalExecutors.executor);
            }
        },
        AS_IS() {
            @Override
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture) {
                return completableFuture.get();
            }
        };

        public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture);

    }
}
