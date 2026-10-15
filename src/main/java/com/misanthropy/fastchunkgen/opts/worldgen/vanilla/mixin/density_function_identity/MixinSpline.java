package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DensityFunctions.Spline.class)
public abstract class MixinSpline {

    @Shadow @Final private CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline;

    @WrapOperation(method = "mapAll", at = @At(value = "NEW", target = "(Lnet/minecraft/util/CubicSpline;)Lnet/minecraft/world/level/levelgen/DensityFunctions$Spline;"))
    private DensityFunctions.Spline fastchunkgen$keepUnchanged(CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline, Operation<DensityFunctions.Spline> original) {
        if (spline == this.spline) return (DensityFunctions.Spline) (Object) this;
        return original.call(spline);
    }

}
