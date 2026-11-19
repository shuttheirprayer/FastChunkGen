package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.release_carving_masks;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.Config;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class MixinReleaseCarvingMasks {
    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void fcg$releaseCarvingMasks(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, CallbackInfo ci) {
        if (Config.releaseCarvingMasksAfterFeatures && chunk instanceof ProtoChunk proto) {
            ((ProtoChunkCarvingAccessor) proto).fcg$carvingMasks().clear();
        }
    }
}
