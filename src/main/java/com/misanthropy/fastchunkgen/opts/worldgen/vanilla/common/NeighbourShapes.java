package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public final class NeighbourShapes {

    private static final Class<?>[] UPDATE_SHAPE_ARGS = {BlockState.class, Direction.class, BlockState.class, LevelAccessor.class, BlockPos.class, BlockPos.class};
    private static final String UPDATE_SHAPE = resolveUpdateShape();

    private static final ClassValue<Boolean> CHANGES_SHAPE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != BlockBehaviour.class; c = c.getSuperclass()) {
                try {
                    c.getDeclaredMethod(UPDATE_SHAPE, UPDATE_SHAPE_ARGS);
                    return true;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    return true;
                }
            }
            return false;
        }
    };

    private NeighbourShapes() {
    }

    public static boolean isStatic(BlockState state) {
        return UPDATE_SHAPE != null && !CHANGES_SHAPE.get(state.getBlock().getClass());
    }

    private static String resolveUpdateShape() {
        try {
            final String name = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_7417_");
            BlockBehaviour.class.getDeclaredMethod(name, UPDATE_SHAPE_ARGS);
            return name;
        } catch (Throwable t) {
            return null;
        }
    }
}
