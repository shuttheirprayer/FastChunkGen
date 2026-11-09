package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public interface StillFluid {

    boolean fcg$cannotSpread(Level level, BlockPos pos, BlockState state);

    static boolean tickIsNoOp(FluidState fluid, Level level, BlockPos pos) {
        if (!fluid.isSource()) return false;
        final Object type = fluid.getType();
        if (type != Fluids.WATER && type != Fluids.LAVA) return false;
        return ((StillFluid) type).fcg$cannotSpread(level, pos, level.getBlockState(pos));
    }
}
