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

    public static final boolean skipNoOpPostProcessing = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.skipNoOpPostProcessing")
            .comment("Skip post-processing fluid ticks and shape updates that provably change nothing.")
            .getBoolean(true, false);

    public static final boolean flatClimateSearch = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.flatClimateSearch")
            .comment("Faster biome parameter lookup with the same results.")
            .getBoolean(true, false);

    public static final boolean columnClimateCache = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.columnClimateCache")
            .comment("Sample height-independent climate noise once per biome column.")
            .getBoolean(true, false);

    public static final boolean reuseSurfaceRules = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.reuseSurfaceRules")
            .comment("Build the surface rule tree once per worker and some less GC churns.")
            .getBoolean(true, false);

    public static final boolean releaseInterpolationSlices = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.releaseInterpolationSlices")
            .comment("Hold the terrain noise interpolation arrays only while a chunk's terrain is being filled instead of for as long as the unfinished chunk exists.")
            .getBoolean(true, false);

    public static boolean releaseNoiseChunkAfterCarving = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.releaseNoiseChunkAfterCarving")
            .comment("Drop a chunk's terrain noise state once its caves are carved instead of keeping it until the chunk is finished. Nothing reads it after that step, and it is rebuilt if anything asks again.")
            .getBoolean(true, false);


    public static boolean releaseCarvingMasksAfterFeatures = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.releaseCarvingMasksAfterFeatures")
            .comment("Drop a chunk's cave carving masks once its features are placed instead of keeping them until the chunk is finished. Features are the last step that reads them, and an unfinished chunk keeps two of them, 24 KB, for as long as it waits at the edge of the loaded area.")
            .getBoolean(true, false);

}
