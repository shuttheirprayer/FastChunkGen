package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.SurfaceWrites;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceSystem$1")
public abstract class MixinSurfaceColumn {

    @Redirect(method = "getBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 0)
    private BlockState fcg$fastGet(ChunkAccess chunk, BlockPos pos) {
        return SurfaceWrites.get(chunk, pos);
    }

    @Redirect(method = "setBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"), require = 0)
    private BlockState fcg$fastSet(ChunkAccess chunk, BlockPos pos, BlockState state, boolean moved) {
        return SurfaceWrites.set(chunk, pos, state, moved);
    }

}
