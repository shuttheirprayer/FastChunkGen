package com.misanthropy.fastchunkgen.opts.worldgen.vanilla;

import com.misanthropy.fastchunkgen.base.common.ModuleMixinPlugin;
import com.misanthropy.fastchunkgen.base.common.compat.ModCompat;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.Config;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.startsWith("com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.aquifer."))
            return Config.optimizeAquifer;

        if (mixinClassName.startsWith("com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.the_end_biome_cache."))
            return Config.useEndBiomeCache;

        if (mixinClassName.startsWith("com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_hash."))
            return Config.cacheDensityFunctionHashes;

        if (mixinClassName.startsWith("com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.noise_chunk_wrap."))
            return Config.skipRedundantDensityFunctionWrapping
                    && !ModCompat.isModernFixFeatureEnabled("mixin.perf.worldgen_allocation");

        return true;
    }
}
