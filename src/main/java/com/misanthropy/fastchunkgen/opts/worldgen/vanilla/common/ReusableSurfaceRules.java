package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface_rules.ISurfaceRulesContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

import java.util.function.Function;

public final class ReusableSurfaceRules {

    private static final ThreadLocal<?> MODERNFIX_POSSIBLE_BIOMES = findModernFixPossibleBiomes();

    private final SurfaceRules.RuleSource source;
    private final RandomState randomState;
    private final int minY;
    private final int height;
    private final SurfaceRules.Context context;
    private final SurfaceRules.SurfaceRule rule;

    public ReusableSurfaceRules(SurfaceRules.RuleSource source, RandomState randomState, WorldGenerationContext generationContext, SurfaceRules.Context context, SurfaceRules.SurfaceRule rule) {
        this.source = source;
        this.randomState = randomState;
        this.minY = generationContext.getMinGenY();
        this.height = generationContext.getGenDepth();
        this.context = context;
        this.rule = rule;
    }

    public boolean matches(SurfaceRules.RuleSource source, RandomState randomState, WorldGenerationContext generationContext) {
        return this.source == source
                && this.randomState == randomState
                && this.minY == generationContext.getMinGenY()
                && this.height == generationContext.getGenDepth();
    }

    public SurfaceRules.Context context() {
        return this.context;
    }

    public SurfaceRules.SurfaceRule rule() {
        return this.rule;
    }

    public void repoint(SurfaceRules.Context fresh) {
        final ISurfaceRulesContext from = (ISurfaceRulesContext) (Object) fresh;
        this.repoint(from.fcg$getChunk(), from.fcg$getNoiseChunk(), from.fcg$getBiomeGetter(), from.fcg$getGenerationContext());
    }

    public void repoint(ChunkAccess chunk, NoiseChunk noiseChunk, Function<BlockPos, Holder<Biome>> biomeGetter, WorldGenerationContext generationContext) {
        final ISurfaceRulesContext to = (ISurfaceRulesContext) (Object) this.context;
        to.fcg$setChunk(chunk);
        to.fcg$setNoiseChunk(noiseChunk);
        to.fcg$setBiomeGetter(biomeGetter);
        to.fcg$setGenerationContext(generationContext);
        to.fcg$setLastPreliminarySurfaceCellOrigin(Long.MAX_VALUE);
    }

    public void release() {
        final ISurfaceRulesContext to = (ISurfaceRulesContext) (Object) this.context;
        to.fcg$setChunk(null);
        to.fcg$setNoiseChunk(null);
        to.fcg$setBiomeGetter(null);
        to.fcg$setGenerationContext(null);
    }

    public static boolean hasModernFixPossibleBiomes() {
        return MODERNFIX_POSSIBLE_BIOMES != null && MODERNFIX_POSSIBLE_BIOMES.get() != null;
    }

    private static ThreadLocal<?> findModernFixPossibleBiomes() {
        try {
            final Class<?> holder = Class.forName("org.embeddedt.modernfix.world.gen.ExtendedSurfaceContext", true, ReusableSurfaceRules.class.getClassLoader());
            return (ThreadLocal<?>) holder.getField("COMPUTED_POSSIBLE_BIOMES").get(null);
        } catch (Throwable t) {
            return null;
        }
    }

}
