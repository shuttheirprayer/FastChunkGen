package com.misanthropy.fastchunkgen.threading.worldgen.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.fastchunkgen.base.common.GlobalExecutors;
import com.misanthropy.fastchunkgen.base.common.scheduler.ThreadLocalWorldGenSchedulingState;
import com.misanthropy.fastchunkgen.threading.worldgen.common.Config;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow
    @Nullable
    protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    @Shadow protected abstract CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkHolder chunkHolder, int margin, IntFunction<ChunkStatus> distanceToStatus);

    /**
     * @author ishland
     * @reason reduce scheduling overhead
     */
    @SuppressWarnings("OverwriteTarget")
    @Dynamic
    @Overwrite(remap = false)
    private void m_214956_(ChunkHolder chunkHolder, Runnable runnable) { // synthetic method for worldGenExecutor scheduling in upgradeChunk
        runnable.run();
    }

    @Dynamic
    @Inject(method = {"method_17225", "lambda$scheduleChunkGeneration$27", "m_279891_"}, at = @At("HEAD"))
    private void captureUpgradingChunkHolder(CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir, @Local(argsOnly = true) ChunkHolder chunkHolder) {
        ThreadLocalWorldGenSchedulingState.setChunkHolder(chunkHolder);
    }

    @Dynamic
    @Inject(method = {"method_17225", "lambda$scheduleChunkGeneration$27", "m_279891_"}, at = @At("RETURN"))
    private void resetUpgradingChunkHolder(CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        ThreadLocalWorldGenSchedulingState.clearChunkHolder();
    }

    @Dynamic
    @Inject(method = {"method_17225", "lambda$scheduleChunkGeneration$27", "m_279891_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;", shift = At.Shift.BEFORE))
    private void resetUpgradingChunkHolderExceptionally(CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        ThreadLocalWorldGenSchedulingState.clearChunkHolder();
    }

    @Redirect(method = "scheduleChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;getChunkRangeFuture(Lnet/minecraft/server/level/ChunkHolder;ILjava/util/function/IntFunction;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> redirectGetRegion(ChunkMap instance, ChunkHolder chunkHolder, int margin, IntFunction<ChunkStatus> distanceToStatus) {
        if (instance != (Object) this) throw new IllegalStateException();
        return chunkHolder.getOrScheduleFuture(distanceToStatus.apply(0), (ChunkMap) (Object) this)
                .thenComposeAsync(unused -> this.getChunkRangeFuture(chunkHolder, margin, distanceToStatus), r -> {
                    if (Config.asyncScheduling) {
                        if (this.mainThreadExecutor.isSameThread()) {
                            GlobalExecutors.executor.execute(r);
                        } else {
                            r.run();
                        }
                    } else {
                        this.mainThreadExecutor.execute(r);
                    }
                });
    }

    @Redirect(method = "getChunkRangeFuture", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;getUpdatingChunkIfPresent(J)Lnet/minecraft/server/level/ChunkHolder;"))
    private ChunkHolder redirectGetChunkHolder(ChunkMap instance, long pos) {
        return this.visibleChunkMap.get(pos); // thread-safe
    }

}
