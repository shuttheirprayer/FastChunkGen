package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Clamp")
public abstract class MixinClamp {

    @Shadow @Final private DensityFunction input;

    @Coerce
    @WrapOperation(method = "mapAll", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/levelgen/DensityFunction;DD)Lnet/minecraft/world/level/levelgen/DensityFunctions$Clamp;"))
    private DensityFunction fastchunkgen$keepUnchanged(DensityFunction input, double min, double max, Operation<DensityFunction> original) {
        if (input == this.input) return (DensityFunction) (Object) this;
        return original.call(input, min, max);
    }

}
