package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.release_carving_masks;

import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ProtoChunk.class)
public interface ProtoChunkCarvingAccessor {
    @Accessor("carvingMasks")
    Map<GenerationStep.Carving, CarvingMask> fcg$carvingMasks();
}
