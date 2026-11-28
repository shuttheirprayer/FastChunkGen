package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.post_processing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.NeighbourShapes;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ShortcutVerify;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.StillFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    @Unique
    private int fcg$postChecks;

    @WrapOperation(method = "postProcessGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void fcg$skipStillFluidTick(FluidState fluid, Level level, BlockPos pos, Operation<Void> original) {
        if (ShortcutVerify.FLUID.broken() || !StillFluid.tickIsNoOp(fluid, level, pos)) {
            original.call(fluid, level, pos);
        } else if (ShortcutVerify.ENABLED || ShortcutVerify.due(++this.fcg$postChecks)) {
            final BlockState[] before = new BlockState[Direction.values().length + 1];
            for (Direction direction : Direction.values()) before[direction.ordinal()] = level.getBlockState(pos.relative(direction));
            before[before.length - 1] = level.getBlockState(pos);
            original.call(fluid, level, pos);
            boolean same = before[before.length - 1] == level.getBlockState(pos);
            for (Direction direction : Direction.values()) same &= before[direction.ordinal()] == level.getBlockState(pos.relative(direction));
            if (ShortcutVerify.ENABLED) ShortcutVerify.FLUID.record(same, pos);
            else if (!same) ShortcutVerify.FLUID.broke(pos);
        }
    }

    @WrapOperation(method = "postProcessGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateFromNeighbourShapes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState fcg$skipStaticShapes(BlockState state, LevelAccessor level, BlockPos pos, Operation<BlockState> original) {
        if (ShortcutVerify.SHAPES.broken() || !NeighbourShapes.isStatic(state)) return original.call(state, level, pos);
        if (ShortcutVerify.ENABLED) ShortcutVerify.SHAPES.record(original.call(state, level, pos) == state, pos);
        else if (ShortcutVerify.due(++this.fcg$postChecks)) {
            final BlockState actual = original.call(state, level, pos);
            if (actual != state) {
                ShortcutVerify.SHAPES.broke(pos);
                return actual;
            }
        }
        return state;
    }
}
