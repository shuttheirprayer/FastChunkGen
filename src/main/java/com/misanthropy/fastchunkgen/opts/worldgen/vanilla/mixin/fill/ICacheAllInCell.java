package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill;

import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.level.levelgen.NoiseChunk$CacheAllInCell")
public interface ICacheAllInCell {

    @Accessor("noiseFiller")
    DensityFunction fcg$noiseFiller();

    @Accessor("values")
    double[] fcg$values();

}
