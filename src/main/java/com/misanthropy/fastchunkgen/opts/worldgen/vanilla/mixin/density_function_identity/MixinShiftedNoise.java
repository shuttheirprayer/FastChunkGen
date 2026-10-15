package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise")
public abstract class MixinShiftedNoise {

    @Shadow @Final private DensityFunction shiftX;
    @Shadow @Final private DensityFunction shiftY;
    @Shadow @Final private DensityFunction shiftZ;
    @Shadow @Final private DensityFunction.NoiseHolder noise;

    @Coerce
    @WrapOperation(method = "mapAll", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;DDLnet/minecraft/world/level/levelgen/DensityFunction$NoiseHolder;)Lnet/minecraft/world/level/levelgen/DensityFunctions$ShiftedNoise;"))
    private DensityFunction fastchunkgen$keepUnchanged(DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise, Operation<DensityFunction> original) {
        if (shiftX == this.shiftX && shiftY == this.shiftY && shiftZ == this.shiftZ && noise == this.noise) return (DensityFunction) (Object) this;
        return original.call(shiftX, shiftY, shiftZ, xzScale, yScale, noise);
    }

}
