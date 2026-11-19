package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.release_noise_chunk;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkAccess.class)
public interface ChunkAccessNoiseAccessor {
    @Accessor("noiseChunk")
    void fcg$setNoiseChunk(NoiseChunk noiseChunk);
}
