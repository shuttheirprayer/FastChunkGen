package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.misanthropy.fastchunkgen.base.common.util.C2MEForkJoinWorkerThreadFactory;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

@Mixin(DensityFunctions.Spline.class)
public abstract class MixinSpline {

    @Shadow @Final private CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline;

    @WrapOperation(method = "mapAll", at = @At(value = "NEW", target = "(Lnet/minecraft/util/CubicSpline;)Lnet/minecraft/world/level/levelgen/DensityFunctions$Spline;"))
    private DensityFunctions.Spline fastchunkgen$keepUnchanged(CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline, Operation<DensityFunctions.Spline> original) {
        if (spline == this.spline) return (DensityFunctions.Spline) (Object) this;
        return original.call(spline);
    }

    /**
     * @author fastchunkgen
     * @reason wrap it once instead of once per sample (identical terrain
     */
    @Overwrite
    public double compute(DensityFunction.FunctionContext context) {
        if (!(Thread.currentThread() instanceof C2MEForkJoinWorkerThreadFactory.C2MEForkJoinWorkerThread worker)) {
            return this.spline.apply(new DensityFunctions.Spline.Point(context));
        }
        int depth = worker.splineDepth;
        Object[] pool = worker.splinePoints;
        if (depth >= pool.length) {
            pool = worker.splinePoints = Arrays.copyOf(pool, pool.length * 2);
        }
        DensityFunctions.Spline.Point point = (DensityFunctions.Spline.Point) pool[depth];
        if (point == null) {
            point = new DensityFunctions.Spline.Point(context);
            pool[depth] = point;
        } else if (point.context() != context) {
            ((ISplinePoint) (Object) point).fcg$setContext(context);
        }
        worker.splineDepth = depth + 1;
        try {
            return this.spline.apply(point);
        } finally {
            worker.splineDepth = depth;
        }
    }

}
