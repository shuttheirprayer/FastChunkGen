package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill;

import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseChunk.NoiseInterpolator.class)
public interface INoiseInterpolator {

    @Accessor("slice0")
    double[][] fcg$slice0();

    @Accessor("slice1")
    double[][] fcg$slice1();

}
