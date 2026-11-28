package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.post_processing_setblock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.NeighbourShapes;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ShortcutVerify;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk {

    @Unique private long fcg$unchangedPos = Long.MIN_VALUE;
    @Unique private boolean fcg$fluidMayChange;

    @WrapOperation(method = "postProcessGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void fcg$noteFluidTick(FluidState fluid, Level level, BlockPos pos, Operation<Void> original) {
        this.fcg$fluidMayChange = !fluid.isSource();
        original.call(fluid, level, pos);
    }

    @WrapOperation(method = "postProcessGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateFromNeighbourShapes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState fcg$noteUnchangedShape(BlockState state, LevelAccessor level, BlockPos pos, Operation<BlockState> original) {
        final boolean fluidMayChange = this.fcg$fluidMayChange;
        this.fcg$fluidMayChange = false;
        final BlockState result = original.call(state, level, pos);
        this.fcg$unchangedPos = result == state && !fluidMayChange && NeighbourShapes.isStatic(state) ? pos.asLong() : Long.MIN_VALUE;
        return result;
    }

    @WrapOperation(method = "postProcessGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean fcg$skipUnchangedSetBlock(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        if (this.fcg$unchangedPos != pos.asLong()) return original.call(level, pos, state, flags);
        this.fcg$unchangedPos = Long.MIN_VALUE;
        if (ShortcutVerify.ENABLED) {
            final BlockState before = level.getBlockState(pos);
            final boolean changed = original.call(level, pos, state, flags);
            ShortcutVerify.SET_BLOCK.record(!changed && before == state && level.getBlockState(pos) == state, pos);
            return changed;
        }
        return false;
    }
}
