package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$MulOrAdd")
public abstract class MixinMulOrAdd {

    @Shadow
    @Final
    private DensityFunction input;

    @Coerce
    @WrapOperation(
            method = "mapAll",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/level/levelgen/DensityFunctions$MulOrAdd$Type;Lnet/minecraft/world/level/levelgen/DensityFunction;DDD)Lnet/minecraft/world/level/levelgen/DensityFunctions$MulOrAdd;"
            )
    )
    private DensityFunction fastchunkgen$keepUnchanged(@Coerce Object specificType, DensityFunction mapped, double minValue, double maxValue, double argument, Operation<DensityFunction> original) {
        if (mapped == this.input) {
            return (DensityFunction) (Object) this;
        }
        return original.call(specificType, mapped, minValue, maxValue, argument);
    }

}
