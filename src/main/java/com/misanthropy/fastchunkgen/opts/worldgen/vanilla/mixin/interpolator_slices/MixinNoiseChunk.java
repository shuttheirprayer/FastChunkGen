package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.interpolator_slices;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.InterpolatorSlices;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk {

    @Shadow @Final private List<NoiseChunk.NoiseInterpolator> interpolators;
    @Shadow @Final int cellCountXZ;
    @Shadow @Final int cellCountY;

    @Inject(method = "initializeForFirstCellX", at = @At("HEAD"))
    private void fcg$acquireSlices(CallbackInfo ci) {
        final List<NoiseChunk.NoiseInterpolator> interpolators = this.interpolators;
        for (int i = 0, n = interpolators.size(); i < n; i++) {
            ((InterpolatorSlices) interpolators.get(i)).fcg$acquireSlices(this.cellCountY, this.cellCountXZ);
        }
    }

    @Inject(method = "stopInterpolation", at = @At("TAIL"))
    private void fcg$releaseSlices(CallbackInfo ci) {
        final List<NoiseChunk.NoiseInterpolator> interpolators = this.interpolators;
        for (int i = 0, n = interpolators.size(); i < n; i++) {
            ((InterpolatorSlices) interpolators.get(i)).fcg$releaseSlices();
        }
    }

}
