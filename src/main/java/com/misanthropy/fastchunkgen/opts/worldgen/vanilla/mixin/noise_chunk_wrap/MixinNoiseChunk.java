package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.noise_chunk_wrap;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Function;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk {

    @Redirect(
            method = "wrap",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"
            )
    )
    private Object onWrapComputeIfAbsent(Map<DensityFunction, DensityFunction> cache, Object key, Function<DensityFunction, DensityFunction> wrapNew) {
        if (key instanceof DensityFunctions.MarkerOrMarked) {
            return cache.computeIfAbsent((DensityFunction) key, wrapNew);
        }
        return wrapNew.apply((DensityFunction) key);
    }

}
