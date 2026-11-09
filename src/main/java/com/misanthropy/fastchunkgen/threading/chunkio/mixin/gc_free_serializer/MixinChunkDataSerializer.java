package com.misanthropy.fastchunkgen.threading.chunkio.mixin.gc_free_serializer;

import com.misanthropy.fastchunkgen.threading.chunkio.common.AsyncSerializationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Pseudo
@Mixin(targets = "com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common.ChunkDataSerializer")
public class MixinChunkDataSerializer {

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntitiesPos()Ljava/util/Set;"))
    private static Set<BlockPos> onChunkGetBlockEntityPositions(ChunkAccess chunk) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        return scope != null ? scope.blockEntityPositions : chunk.getBlockEntitiesPos();
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntityNbtForSaving(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag onChunkGetPackedBlockEntityNbt(ChunkAccess chunk, BlockPos pos) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        if (scope == null) return chunk.getBlockEntityNbtForSaving(pos);
        return scope.blockEntityNbts.get(pos);
    }

    @Redirect(method = "writeSectionDataVanilla", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getLayerListener(Lnet/minecraft/world/level/LightLayer;)Lnet/minecraft/world/level/lighting/LayerLightEventListener;"))
    private static LayerLightEventListener onLightingProviderGet(LevelLightEngine lightingProvider, LightLayer lightType) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(null);
        return scope != null ? scope.lighting.get(lightType) : lightingProvider.getLayerListener(lightType);
    }

}
