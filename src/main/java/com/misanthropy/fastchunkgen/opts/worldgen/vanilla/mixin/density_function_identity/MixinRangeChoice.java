package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$RangeChoice")
public abstract class MixinRangeChoice {

    @Shadow @Final private DensityFunction input;
    @Shadow @Final private DensityFunction whenInRange;
    @Shadow @Final private DensityFunction whenOutOfRange;

    @Coerce
    @WrapOperation(method = "mapAll", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/levelgen/DensityFunction;DDLnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;)Lnet/minecraft/world/level/levelgen/DensityFunctions$RangeChoice;"))
    private DensityFunction fastchunkgen$keepUnchanged(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange, Operation<DensityFunction> original) {
        if (input == this.input && whenInRange == this.whenInRange && whenOutOfRange == this.whenOutOfRange) return (DensityFunction) (Object) this;
        return original.call(input, minInclusive, maxExclusive, whenInRange, whenOutOfRange);
    }

}
