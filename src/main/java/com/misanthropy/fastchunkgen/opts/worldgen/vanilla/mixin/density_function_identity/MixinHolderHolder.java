package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DensityFunctions.HolderHolder.class)
public abstract class MixinHolderHolder {

    @Shadow
    @Final
    private Holder<DensityFunction> function;

    /**
     * @author Misanthropy
     * @reason NoiseChunk unwraps the holder pair straight away, so do not build it for that visitor at all
     */
    @Overwrite
    public DensityFunction mapAll(DensityFunction.Visitor visitor) {
        DensityFunction mapped = this.function.value().mapAll(visitor);
        if (visitor instanceof NoiseChunk) {
            return mapped;
        }
        return visitor.apply(new DensityFunctions.HolderHolder(new Holder.Direct<>(mapped)));
    }

}
