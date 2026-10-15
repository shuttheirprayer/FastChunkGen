package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface;

import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeManager.class)
public interface IBiomeManager {

    @Accessor("noiseBiomeSource")
    BiomeManager.NoiseBiomeSource fcg$getNoiseBiomeSource();

}
