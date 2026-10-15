package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DensityFunctions.Spline.Coordinate.class)
public abstract class MixinSplineCoordinate {

    @Shadow @Final private Holder<DensityFunction> function;

    @WrapOperation(method = "mapAll", at = @At(value = "NEW", target = "(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/level/levelgen/DensityFunctions$Spline$Coordinate;"))
    private DensityFunctions.Spline.Coordinate fastchunkgen$keepUnchanged(Holder<DensityFunction> mapped, Operation<DensityFunctions.Spline.Coordinate> original) {
        // vanilla rebuilds
        if (this.function instanceof Holder.Direct<?> && mapped.value() == this.function.value()) return (DensityFunctions.Spline.Coordinate) (Object) this;
        return original.call(mapped);
    }

}
