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
            .comment("Cache the hashCode of density function and spline records. They are immutable, so the value never changes.")
            .getBoolean(true, false);

    public static final boolean skipRedundantDensityFunctionWrapping = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.skipRedundantDensityFunctionWrapping")
            .comment("Skip the NoiseChunk wrapper map for density functions it would hand straight back. Only marker nodes own a cache that has to be shared.")
            .getBoolean(true, false);

    public static final boolean identityPreservingDensityFunctionMapAll = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.identityPreservingDensityFunctionMapAll")
            .comment("Reuse density function nodes whose inputs did not change when a chunk maps the noise router instead of rebuilding every node per chunk, and skip the throwaway holder pair NoiseChunk unwraps immediately.")
            .getBoolean(true, false);

}
