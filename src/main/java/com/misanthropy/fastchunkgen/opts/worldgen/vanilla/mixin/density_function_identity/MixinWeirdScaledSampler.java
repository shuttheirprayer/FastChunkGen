package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$WeirdScaledSampler")
public abstract class MixinWeirdScaledSampler {

    @Shadow @Final private DensityFunction input;
    @Shadow @Final private DensityFunction.NoiseHolder noise;

    @Coerce
    @WrapOperation(method = "mapAll", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction$NoiseHolder;Lnet/minecraft/world/level/levelgen/DensityFunctions$WeirdScaledSampler$RarityValueMapper;)Lnet/minecraft/world/level/levelgen/DensityFunctions$WeirdScaledSampler;"))
    private DensityFunction fastchunkgen$keepUnchanged(DensityFunction input, DensityFunction.NoiseHolder noise, @Coerce Object rarityValueMapper, Operation<DensityFunction> original) {
        if (input == this.input && noise == this.noise) return (DensityFunction) (Object) this;
        return original.call(input, noise, rarityValueMapper);
    }

}
