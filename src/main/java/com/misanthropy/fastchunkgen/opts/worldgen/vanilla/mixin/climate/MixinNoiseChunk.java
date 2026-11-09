package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.climate;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ColumnClimate;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk {

    @Inject(method = "cachedClimateSampler", at = @At("RETURN"), cancellable = true)
    private void fcg$columnClimate(NoiseRouter router, List<Climate.ParameterPoint> spawnTarget, CallbackInfoReturnable<Climate.Sampler> cir) {
        cir.setReturnValue(ColumnClimate.wrap(cir.getReturnValue(), router));
    }
}
