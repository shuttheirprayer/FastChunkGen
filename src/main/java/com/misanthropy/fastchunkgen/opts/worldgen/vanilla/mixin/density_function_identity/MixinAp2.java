package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.misanthropy.fastchunkgen.base.common.util.C2MEForkJoinWorkerThreadFactory;
import com.misanthropy.fastchunkgen.base.common.util.DensityScratch;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.Arrays;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Ap2")
public abstract class MixinAp2 {

    @Shadow
    @Final
    private DensityFunctions.TwoArgumentSimpleFunction.Type type;

    @Shadow
    @Final
    private DensityFunction argument1;

    @Shadow
    @Final
    private DensityFunction argument2;

    @Coerce
    @WrapOperation(
            method = "mapAll",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunctions$TwoArgumentSimpleFunction;create(Lnet/minecraft/world/level/levelgen/DensityFunctions$TwoArgumentSimpleFunction$Type;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;)Lnet/minecraft/world/level/levelgen/DensityFunctions$TwoArgumentSimpleFunction;"
            )
    )
    private DensityFunction fastchunkgen$keepUnchanged(@Coerce Object type, DensityFunction mapped1, DensityFunction mapped2, Operation<DensityFunction> original) {
        if (mapped1 == this.argument1 && mapped2 == this.argument2) {
            return (DensityFunction) (Object) this;
        }
        return original.call(type, mapped1, mapped2);
    }

    /**
     * @author fastchunkgen
     * @reason borrow a per-thread one instead of allocating fresh every dam single time
     */
    @Overwrite
    public void fillArray(double[] out, DensityFunction.ContextProvider provider) {
        this.argument1.fillArray(out, provider);
        DensityFunctions.TwoArgumentSimpleFunction.Type type = this.type;
        DensityFunction argument2 = this.argument2;
        if (type == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD) {
            if (!(Thread.currentThread() instanceof C2MEForkJoinWorkerThreadFactory.C2MEForkJoinWorkerThread worker)) {
                DensityScratch fallback = DensityScratch.fallback();
                int depth = fallback.depth;
                double[][] cache = fallback.scratch;
                double[] scratch = depth < cache.length ? cache[depth] : null;
                if (scratch == null || scratch.length != out.length) {
                    scratch = new double[out.length];
                    if (depth < cache.length) {
                        cache[depth] = scratch;
                    }
                } else {
                    Arrays.fill(scratch, 0.0);
                }
                fallback.depth = depth + 1;
                try {
                    argument2.fillArray(scratch, provider);
                    for (int i = 0; i < out.length; i++) {
                        out[i] += scratch[i];
                    }
                } finally {
                    fallback.depth = depth;
                }
                return;
            }
            int depth = worker.scratchDepth;
            double[][] cache = worker.scratch;
            double[] scratch = depth < cache.length ? cache[depth] : null;
            if (scratch == null || scratch.length != out.length) {
                scratch = new double[out.length];
                if (depth < cache.length) {
                    cache[depth] = scratch;
                }
            } else {
                Arrays.fill(scratch, 0.0);
            }
            worker.scratchDepth = depth + 1;
            try {
                argument2.fillArray(scratch, provider);
                for (int i = 0; i < out.length; i++) {
                    out[i] += scratch[i];
                }
            } finally {
                worker.scratchDepth = depth;
            }
        } else if (type == DensityFunctions.TwoArgumentSimpleFunction.Type.MAX) {
            double max = argument2.maxValue();
            for (int i = 0; i < out.length; i++) {
                double value = out[i];
                out[i] = value > max ? value : Math.max(value, argument2.compute(provider.forIndex(i)));
            }
        } else if (type == DensityFunctions.TwoArgumentSimpleFunction.Type.MIN) {
            double min = argument2.minValue();
            for (int i = 0; i < out.length; i++) {
                double value = out[i];
                out[i] = value < min ? value : Math.min(value, argument2.compute(provider.forIndex(i)));
            }
        } else if (type == DensityFunctions.TwoArgumentSimpleFunction.Type.MUL) {
            for (int i = 0; i < out.length; i++) {
                double value = out[i];
                out[i] = value == 0.0 ? 0.0 : value * argument2.compute(provider.forIndex(i));
            }
        }
    }

}
