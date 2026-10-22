package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DensityFunctions.Spline.Point.class)
public interface ISplinePoint {
    @Mutable
    @Accessor("context")
    void fcg$setContext(DensityFunction.FunctionContext context);
}
