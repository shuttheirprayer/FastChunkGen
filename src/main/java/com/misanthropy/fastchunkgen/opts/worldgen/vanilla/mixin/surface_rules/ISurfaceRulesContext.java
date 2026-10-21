package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface_rules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(SurfaceRules.Context.class)
public interface ISurfaceRulesContext {

    @Accessor("randomState")
    RandomState fcg$getRandomState();

    @Accessor("chunk")
    ChunkAccess fcg$getChunk();

    @Accessor("chunk")
    @Mutable
    void fcg$setChunk(ChunkAccess chunk);

    @Accessor("noiseChunk")
    NoiseChunk fcg$getNoiseChunk();

    @Accessor("noiseChunk")
    @Mutable
    void fcg$setNoiseChunk(NoiseChunk noiseChunk);

    @Accessor("biomeGetter")
    Function<BlockPos, Holder<Biome>> fcg$getBiomeGetter();

    @Accessor("biomeGetter")
    @Mutable
    void fcg$setBiomeGetter(Function<BlockPos, Holder<Biome>> biomeGetter);

    @Accessor("context")
    WorldGenerationContext fcg$getGenerationContext();

    @Accessor("context")
    @Mutable
    void fcg$setGenerationContext(WorldGenerationContext context);

    @Accessor("lastPreliminarySurfaceCellOrigin")
    void fcg$setLastPreliminarySurfaceCellOrigin(long origin);

}
