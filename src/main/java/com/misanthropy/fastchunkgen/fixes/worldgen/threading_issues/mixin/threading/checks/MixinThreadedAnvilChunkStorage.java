package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.threading.checks;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public class MixinThreadedAnvilChunkStorage {

    @Shadow
    @Final
    private LongSet entitiesInLevel;

    @Shadow @Final private static Logger LOGGER;

    @Dynamic
    @Inject(method = "m_214854_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;runPostLoad()V"), cancellable = false)
    // lambda expression in convertToFullChunk
    private void afterLoadToWorld(ChunkHolder chunkHolder, ChunkAccess protoChunk, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        if (this.entitiesInLevel.contains(chunkHolder.getPos().toLong()))
            LOGGER.error("Double scheduling chunk loading detected on chunk {}", chunkHolder.getPos());
    }

}
