package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.interpolator_slices;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.InterpolatorSlices;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseChunk.NoiseInterpolator.class)
public abstract class MixinNoiseInterpolator implements InterpolatorSlices {

    @Shadow double[][] slice0;
    @Shadow double[][] slice1;

    @Shadow
    private double[][] allocateSlice(int cellCountY, int cellCountZ) {
        throw new AssertionError();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseChunk$NoiseInterpolator;allocateSlice(II)[[D"))
    private double[][] fcg$deferSlice(NoiseChunk.NoiseInterpolator self, int cellCountY, int cellCountZ) {
        return null;
    }

    @Override
    public void fcg$acquireSlices(int cellCountY, int cellCountZ) {
        if (this.slice0 == null) this.slice0 = this.allocateSlice(cellCountY, cellCountZ);
        if (this.slice1 == null) this.slice1 = this.allocateSlice(cellCountY, cellCountZ);
    }

    @Override
    public void fcg$releaseSlices() {
        this.slice0 = null;
        this.slice1 = null;
    }

}
