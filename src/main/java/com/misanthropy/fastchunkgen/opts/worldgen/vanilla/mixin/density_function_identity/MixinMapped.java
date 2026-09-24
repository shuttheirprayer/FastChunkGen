package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Mapped")
public abstract class MixinMapped {

    @Shadow
    @Final
    private DensityFunction input;

    @Coerce
    @WrapOperation(
            method = "mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/DensityFunctions$Mapped;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunctions$Mapped;create(Lnet/minecraft/world/level/levelgen/DensityFunctions$Mapped$Type;Lnet/minecraft/world/level/levelgen/DensityFunction;)Lnet/minecraft/world/level/levelgen/DensityFunctions$Mapped;"
            )
    )
    private DensityFunction fastchunkgen$keepUnchanged(@Coerce Object type, DensityFunction mapped, Operation<DensityFunction> original) {
        if (mapped == this.input) {
            return (DensityFunction) (Object) this;
        }
        return original.call(type, mapped);
    }

}
