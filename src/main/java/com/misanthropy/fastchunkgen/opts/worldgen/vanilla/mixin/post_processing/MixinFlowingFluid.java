package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.post_processing;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.StillFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FlowingFluid.class)
public abstract class MixinFlowingFluid implements StillFluid {

    @Unique
    private static final Direction[] FCG$SPREAD_DIRECTIONS = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    @Shadow
    public abstract Fluid getFlowing();

    @Shadow
    public abstract Fluid getSource();

    @Shadow
    protected abstract boolean canSpreadTo(BlockGetter level, BlockPos from, BlockState fromState, Direction direction, BlockPos to, BlockState toState, FluidState toFluid, Fluid fluid);

    @Shadow
    private boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos from, BlockState fromState, BlockPos to, BlockState toState) {
        throw new AssertionError();
    }

    @Override
    public boolean fcg$cannotSpread(Level level, BlockPos pos, BlockState state) {
        final Fluid flowing = this.getFlowing();
        final Fluid source = this.getSource();
        for (Direction direction : FCG$SPREAD_DIRECTIONS) {
            final BlockPos to = pos.relative(direction);
            final BlockState toState = level.getBlockState(to);
            final FluidState toFluid = toState.getFluidState();
            if (this.canSpreadTo(level, pos, state, direction, to, toState, toFluid, flowing)) return false;
            if (this.canSpreadTo(level, pos, state, direction, to, toState, toFluid, source)) return false;
            if (!this.canPassThroughWall(direction.getOpposite(), level, to, toState, pos, state)
                    && this.canSpreadTo(level, pos, state, direction, to, toState, toFluid, Fluids.EMPTY)) return false;
        }
        return true;
    }
}
