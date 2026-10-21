package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;

public class Config {

    public static final boolean optimizeAquifer = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.optimizeAquifer")
            .comment("Faster aquifer sampling.")
            .incompatibleMod("cavetweaks", "*")
            .getBoolean(true, false);

    public static final boolean useEndBiomeCache = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.useEndBiomeCache")
            .comment("Cache End biome lookups.")
            .getBoolean(true, false);

    public static final boolean cacheDensityFunctionHashes = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.cacheDensityFunctionHashes")
            .comment("Cache the hashCode of density function and spline records. Their value never changes anyways!")
            .getBoolean(true, false);

    public static final boolean skipRedundantDensityFunctionWrapping = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.skipRedundantDensityFunctionWrapping")
            .comment("Skip the NoiseChunk wrapper map for density functions.")
            .getBoolean(true, false);

    public static final boolean shareNoiseBetweenChunks = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.shareNoiseBetweenChunks")
            .comment("Compute noise that neighbouring chunks both need once instead of in each chunk.")
            .getBoolean(true, false);

    public static final boolean identityPreservingDensityFunctionMapAll = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.identityPreservingDensityFunctionMapAll")
            .comment("Cache unchanged density function nodes during chunk mapping instead of rebuilding them, and eliminate redundant calculations")
            .getBoolean(true, false);

    public static final boolean reuseSurfaceRules = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.reuseSurfaceRules")
            .comment("Build the surface rule tree once per worker and some less GC churns.")
            .getBoolean(true, false);

}
