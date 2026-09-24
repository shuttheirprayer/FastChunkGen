package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_hash;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

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

    @WrapMethod(method = "hashCode")
    private int fastchunkgen$cachedHash(Operation<Integer> original) {
        int cached = this.fastchunkgen$hash;
        if (cached != 0) {
            return cached;
        }
        int computed = original.call();
        this.fastchunkgen$hash = computed;
        return computed;
    }

}
