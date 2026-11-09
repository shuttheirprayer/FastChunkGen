package com.misanthropy.fastchunkgen.threading.worldgen.mixin.cancellation;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> schedule(ChunkHolder holder, ChunkStatus requiredStatus);

    @Shadow @Final private static Logger LOGGER;

    @Redirect(method = "schedule", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkLevel;byStatus(Lnet/minecraft/world/level/chunk/ChunkStatus;)I"))
    private int redirectAddLightTicketDistance(ChunkStatus status) {
        return status == ChunkStatus.LIGHT ? ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS) - 2 : ChunkLevel.byStatus(status);
    }

    @Dynamic
    @Redirect(method = "m_287046_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkLevel;byStatus(Lnet/minecraft/world/level/chunk/ChunkStatus;)I"))
    private int redirectRemoveLightTicketDistance(ChunkStatus status) {
        return status == ChunkStatus.LIGHT ? ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS) - 2 : ChunkLevel.byStatus(status);
    }

    @ModifyReturnValue(method = "schedule", at = @At("RETURN"))
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> injectCancellationHook(CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> originalReturn, ChunkHolder holder, ChunkStatus requiredStatus) {
        return originalReturn.thenCompose(either -> {
            if (either.right().isPresent()) {
                return CompletableFuture.supplyAsync(() -> {
                    if (ChunkLevel.generationStatus(holder.getTicketLevel()).isOrAfter(requiredStatus)) {
//                        LOGGER.info("Chunk load {} raced, recovering", holder.getPos());
                        return this.schedule(holder, requiredStatus); // recover from cancellation
                    } else {
                        return CompletableFuture.completedFuture(either);
                    }
                }, this.mainThreadExecutor).thenCompose(Function.identity());
            } else {
                return CompletableFuture.completedFuture(either);
            }
        });
    }

}
