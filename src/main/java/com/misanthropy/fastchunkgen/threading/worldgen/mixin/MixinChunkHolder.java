package com.misanthropy.fastchunkgen.threading.worldgen.mixin;

import com.misanthropy.fastchunkgen.base.mixin.access.IThreadedAnvilChunkStorage;
import com.misanthropy.fastchunkgen.threading.worldgen.common.Config;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

@Mixin(value = ChunkHolder.class, priority = 1110)
public abstract class MixinChunkHolder {

    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getOrScheduleFuture(ChunkStatus targetStatus, ChunkMap chunkStorage);

    @Redirect(method = "getOrScheduleFuture", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;schedule(Lnet/minecraft/server/level/ChunkHolder;Lnet/minecraft/world/level/chunk/ChunkStatus;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> redirectGetChunk(ChunkMap instance, ChunkHolder holder, ChunkStatus requiredStatus) {
        if (requiredStatus == ChunkStatus.EMPTY) {
            return instance.schedule(holder, requiredStatus);
        } else {
            return this.getOrScheduleFuture(requiredStatus.getParent(), instance)
                    .thenComposeAsync(
                            unused -> instance.schedule(holder, requiredStatus),
                            Config.asyncScheduling ? Runnable::run : r -> {
                                final BlockableEventLoop<Runnable> executor = ((IThreadedAnvilChunkStorage) instance).getMainThreadExecutor();
                                if (executor.isSameThread()) {
                                    r.run();
                                } else {
                                    executor.execute(r);
                                }
                            }
                    );
        }
    }

}
