package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Ap2")
public abstract class MixinAp2 {

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

}
