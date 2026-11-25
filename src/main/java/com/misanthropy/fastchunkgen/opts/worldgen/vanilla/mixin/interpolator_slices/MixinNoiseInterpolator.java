package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.interpolator_slices;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.InterpolatorSlices;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.SlicePool;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
        SlicePool pool = SlicePool.local();
        if (this.slice0 == null) this.slice0 = this.fcg$slice(pool, cellCountY, cellCountZ);
        if (this.slice1 == null) this.slice1 = this.fcg$slice(pool, cellCountY, cellCountZ);
    }

    @Unique
    private double[][] fcg$slice(SlicePool pool, int cellCountY, int cellCountZ) {
        double[][] slice = pool.take(cellCountZ + 1, cellCountY + 1);
        return slice != null ? slice : this.allocateSlice(cellCountY, cellCountZ);
    }

    @Override
    public void fcg$releaseSlices() {
        SlicePool pool = SlicePool.local();
        pool.give(this.slice0);
        pool.give(this.slice1);
        this.slice0 = null;
        this.slice1 = null;
    }

}
