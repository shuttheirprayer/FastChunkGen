package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface_fold;

import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SurfaceRules.Context.class)
public interface ISurfaceContext {

    @Accessor("blockY")
    int fcg$getBlockY();

    @Accessor("context")
    WorldGenerationContext fcg$getGenerationContext();

}
