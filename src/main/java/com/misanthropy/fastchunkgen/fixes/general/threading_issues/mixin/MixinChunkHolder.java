package com.misanthropy.fastchunkgen.fixes.general.threading_issues.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder {

    @Shadow
    @Final
    public static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> UNLOADED_CHUNK_FUTURE;
    @Shadow
    @Final
    public static Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> UNLOADED_CHUNK;
    @Shadow
    private int ticketLevel;

    @Shadow
    protected abstract void updateChunkToSave(CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc);

    @Shadow
    @Final
    private AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures;
    @Shadow private CompletableFuture<ChunkAccess> chunkToSave;
    @Unique
    private Object schedulingMutex = new Object();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.schedulingMutex = new Object();
    }

    /**
     * @author ishland
     * @reason improve handling of async chunk request
     */
    @Overwrite
    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getOrScheduleFuture(ChunkStatus targetStatus, ChunkMap chunkStorage) {
        // TODO [VanillaCopy]
        int i = targetStatus.getIndex();
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.futures.get(i);
        if (completableFuture != null && !fcg$needsReschedule(completableFuture)) {
            return completableFuture;
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future;

        synchronized (this.schedulingMutex) {
            // copied from above
            completableFuture = this.futures.get(i);
            if (completableFuture != null && !fcg$needsReschedule(completableFuture)) {
                return completableFuture;
            }
            if (ChunkLevel.generationStatus(this.ticketLevel).isOrAfter(targetStatus)) {
                future = new CompletableFuture<>();
                this.futures.set(i, future);
                // C2ME - moved down to prevent deadlock
            } else {
                return completableFuture == null ? UNLOADED_CHUNK_FUTURE : completableFuture;
            }
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture2;
        try {
            completableFuture2 = chunkStorage.schedule((ChunkHolder) (Object) this, targetStatus);
        } catch (Throwable t) {
            synchronized (this.schedulingMutex) {
                this.futures.compareAndSet(i, future, null);
            }
            future.completeExceptionally(t);
            throw t;
        }
        // synchronization: see below
        synchronized (this) {
            this.updateChunkToSave(completableFuture2, "schedule " + targetStatus);
        }
        completableFuture2.whenComplete((either, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }
            future.complete(either);
        });
        this.futures.set(i, completableFuture2);
        completableFuture2.whenComplete((either, throwable) -> {
            if (throwable != null) {
                synchronized (this.schedulingMutex) {
                    this.futures.compareAndSet(i, completableFuture2, null);
                }
            }
        });
        return completableFuture2;
    }

    @Unique
    private static boolean fcg$needsReschedule(CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future) {
        if (future.isCompletedExceptionally()) return true;
        final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = future.getNow(null);
        return either != null && either.right().isPresent();
    }

    @Dynamic
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;updateChunkToSave(Ljava/util/concurrent/CompletableFuture;Ljava/lang/String;)V"))
    private void synchronizeCombineSavingFuture(ChunkHolder holder, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc) {
        synchronized (this) {
            // this!!
            this.updateChunkToSave(then.exceptionally(unused -> UNLOADED_CHUNK), thenDesc);
        }
    }

    /**
     * @author ishland
     * @reason synchronize
     */
    @Overwrite
    public void addSaveDependency(String string, CompletableFuture<?> completableFuture) {
        synchronized (this) {
            this.chunkToSave = this.chunkToSave.thenCombine(completableFuture.exceptionally(unused -> null), (chunk, object) -> chunk);
        }
    }

}
