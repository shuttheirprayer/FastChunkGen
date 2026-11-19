package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.release_noise_chunk;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MixinReleaseNoiseChunk {
    @Inject(method = "applyCarvers", at = @At("RETURN"))
    private void fcg$releaseNoiseChunk(net.minecraft.server.level.WorldGenRegion region, long seed, net.minecraft.world.level.levelgen.RandomState randomState, net.minecraft.world.level.biome.BiomeManager biomeManager, net.minecraft.world.level.StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step, CallbackInfo ci) {
        if (com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.Config.releaseNoiseChunkAfterCarving) {
            ((ChunkAccessNoiseAccessor) chunk).fcg$setNoiseChunk(null);
        }
    }
}
