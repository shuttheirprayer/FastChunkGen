package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_hash;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "net.minecraft.util.CubicSpline$Multipoint",
        "net.minecraft.world.level.levelgen.DensityFunction$NoiseHolder",
        "net.minecraft.world.level.levelgen.DensityFunctions$Ap2",
        "net.minecraft.world.level.levelgen.DensityFunctions$BlendDensity",
        "net.minecraft.world.level.levelgen.DensityFunctions$Clamp",
        "net.minecraft.world.level.levelgen.DensityFunctions$HolderHolder",
        "net.minecraft.world.level.levelgen.DensityFunctions$Mapped",
        "net.minecraft.world.level.levelgen.DensityFunctions$Marker",
        "net.minecraft.world.level.levelgen.DensityFunctions$MulOrAdd",
        "net.minecraft.world.level.levelgen.DensityFunctions$Noise",
        "net.minecraft.world.level.levelgen.DensityFunctions$RangeChoice",
        "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise",
        "net.minecraft.world.level.levelgen.DensityFunctions$Spline",
        "net.minecraft.world.level.levelgen.DensityFunctions$Spline$Coordinate",
        "net.minecraft.world.level.levelgen.DensityFunctions$WeirdScaledSampler"
})
public abstract class MixinCachedRecordHash {

    @Unique
    private int fastchunkgen$hash;

    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void onHashCodeHead(CallbackInfoReturnable<Integer> cir) {
        final int cached = this.fastchunkgen$hash;
        if (cached != 0) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"))
    private void onHashCodeReturn(CallbackInfoReturnable<Integer> cir) {
        this.fastchunkgen$hash = cir.getReturnValue();
    }

}
