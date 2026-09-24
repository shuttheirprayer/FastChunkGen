package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Noise")
public abstract class MixinNoise {

    @Shadow
    @Final
    private DensityFunction.NoiseHolder noise;

    @Coerce
    @WrapOperation(
            method = "mapAll",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/level/levelgen/DensityFunction$NoiseHolder;DD)Lnet/minecraft/world/level/levelgen/DensityFunctions$Noise;"
            )
    )
    private DensityFunction fastchunkgen$keepUnchanged(DensityFunction.NoiseHolder visited, double xzScale, double yScale, Operation<DensityFunction> original) {
        if (visited == this.noise) {
            return (DensityFunction) (Object) this;
        }
        return original.call(visited, xzScale, yScale);
    }

}
