package com.misanthropy.fastchunkgen.opts.chunk_access.mixin.async_chunk_request;

import com.misanthropy.fastchunkgen.base.common.util.CFUtil;
import com.misanthropy.fastchunkgen.base.common.util.Log;
import com.misanthropy.fastchunkgen.opts.chunk_access.common.CurrentWorldGenState;
import com.mojang.datafixers.util.Either;
import net.minecraft.Util;
import net.minecraft.server.level.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkManager {

    @Shadow
    @Final
    private Thread mainThread;

    @Shadow
    @Nullable
    protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow
    @Final
    private DistanceManager distanceManager;

    @Shadow
    protected abstract boolean chunkAbsent(@Nullable ChunkHolder holder, int maxLevel);

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    public abstract boolean runDistanceManagerUpdates();

    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final public ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    private static final TicketType<ChunkPos> ASYNC_LOAD = TicketType.create("async_load", Comparator.comparingLong(ChunkPos::toLong));
    private static final long NESTED_LOAD_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30L);
    private static final long NESTED_LOAD_REPORT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30L);
    private static final AtomicLong NESTED_LOAD_LAST_REPORT = new AtomicLong(System.nanoTime() - NESTED_LOAD_REPORT_INTERVAL_NANOS - 1L);

    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
    private void onGetChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (Thread.currentThread() != this.mainThread) {
            cir.setReturnValue(fcg$getChunkOffThread(chunkX, chunkZ, leastStatus, create));
        }
    }

    @Unique
    @Final
    private ChunkAccess fcg$getChunkOffThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        final WorldGenRegion currentRegion = CurrentWorldGenState.getCurrentRegion();
        if (currentRegion != null) {
            ChunkAccess chunk = currentRegion.getChunk(chunkX, chunkZ, leastStatus, false);
            if (chunk instanceof ImposterProtoChunk readOnlyChunk) chunk = readOnlyChunk.getWrapped();
            if (chunk != null) return chunk;
            chunk = fcg$getChunkIfPresent(chunkX, chunkZ, leastStatus);
            if (chunk != null) return chunk;
        }
        final CompletableFuture<ChunkAccess> chunkLoad = fcg$getChunkFutureOffThread(chunkX, chunkZ, leastStatus, create);
        assert chunkLoad != null;
        if (currentRegion == null) return CFUtil.join(chunkLoad);
        final long timeoutNanos = currentRegion.hasChunk(chunkX, chunkZ) ? 0L : NESTED_LOAD_TIMEOUT_NANOS;
        if (CFUtil.await(chunkLoad, timeoutNanos)) return chunkLoad.join();
        final ChunkAccess present = fcg$getChunkIfPresent(chunkX, chunkZ, leastStatus);
        if (present != null) return present;
        if (!create) return null;
        fcg$reportNestedLoad(currentRegion, chunkX, chunkZ, leastStatus);
        throw new CancellationException("Chunk [%d, %d] at status %s cannot be reached from world generation of chunk %s"
                .formatted(chunkX, chunkZ, leastStatus, currentRegion.getCenter()));
    }

    @Unique
    private static void fcg$reportNestedLoad(WorldGenRegion region, int chunkX, int chunkZ, ChunkStatus leastStatus) {
        final long now = System.nanoTime();
        final long last = NESTED_LOAD_LAST_REPORT.get();
        if (now - last < NESTED_LOAD_REPORT_INTERVAL_NANOS || !NESTED_LOAD_LAST_REPORT.compareAndSet(last, now)) return;
        Log.THREADED_WORLDGEN.warn(
                "A mod requested chunk [{}, {}] at status {} while chunk {} was still being generated. Waiting for it would deadlock chunk generation, so generation of {} is cancelled and will be retried. The mod in the stacktrace below should read from the world generation region it was handed instead of from the level.",
                chunkX, chunkZ, leastStatus, region.getCenter(), region.getCenter(), new Throwable());
    }

    @Unique
    @Nullable
    private ChunkAccess fcg$getChunkIfPresent(int chunkX, int chunkZ, ChunkStatus leastStatus) {
        final ChunkHolder holder = this.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
        if (holder == null) return null;
        final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = holder.getFutureIfPresentUnchecked(leastStatus).getNow(null);
        return either != null ? either.left().orElse(null) : null;
    }

    @Unique
    @Final
    @Nullable
    private CompletableFuture<ChunkAccess> fcg$getChunkFutureOffThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        return CompletableFuture.supplyAsync(() -> {
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            long chunkPosLong = chunkPos.toLong();
            int ticketLevel = 33 + ChunkStatus.getDistance(leastStatus);
            ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPosLong);
            boolean doCreate = create && (chunkHolder == null || this.chunkAbsent(chunkHolder, ticketLevel));
            if (doCreate) {
                this.distanceManager.addTicket(ASYNC_LOAD, chunkPos, ticketLevel, chunkPos);
                if (this.chunkAbsent(chunkHolder, ticketLevel)) {
                    ProfilerFiller profiler = this.level.getProfiler();
                    profiler.push("chunkLoad");
                    this.runDistanceManagerUpdates();
                    chunkHolder = this.getVisibleChunkIfPresent(chunkPosLong);
                    profiler.pop();
                    if (this.chunkAbsent(chunkHolder, ticketLevel)) {
                        this.distanceManager.removeTicket(ASYNC_LOAD, chunkPos, ticketLevel, chunkPos);
                        throw Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                    }
                }
            }

            final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = this.chunkAbsent(chunkHolder, ticketLevel) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : chunkHolder.getOrScheduleFuture(leastStatus, this.chunkMap);
            if (doCreate && future != null) {
                future.exceptionally(__ -> null).thenRunAsync(() -> {
                    this.distanceManager.removeTicket(ASYNC_LOAD, chunkPos, ticketLevel, chunkPos);
                }, this.mainThreadProcessor);
            }
            return future;
        }, this.mainThreadProcessor).thenCompose(Function.identity()).thenApply(either -> either.map(Function.identity(), unloaded -> {
            if (create) {
                throw Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + unloaded));
            } else {
                return null;
            }
        }));
    }



}
