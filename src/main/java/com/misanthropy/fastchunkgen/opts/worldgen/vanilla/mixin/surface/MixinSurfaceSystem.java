package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface;

import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.UniformCellBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Function;

@Mixin(SurfaceSystem.class)
public class MixinSurfaceSystem {

    @ModifyArg(method = "buildSurface", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;<init>(Lnet/minecraft/world/level/levelgen/SurfaceSystem;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Ljava/util/function/Function;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;)V"), index = 4, require = 0)
    private Function<BlockPos, Holder<Biome>> fcg$uniformCellBiomes(Function<BlockPos, Holder<Biome>> biomeGetter, @Local(argsOnly = true) BiomeManager biomeManager) {
        return new UniformCellBiomes(((IBiomeManager) biomeManager).fcg$getNoiseBiomeSource(), biomeGetter);
    }

}
