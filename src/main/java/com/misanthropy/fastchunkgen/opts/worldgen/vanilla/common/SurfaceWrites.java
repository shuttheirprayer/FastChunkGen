package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

public final class SurfaceWrites {

    public interface FastChunk {
        int fcg$minY();

        int fcg$maxY();

        int fcg$minSection();

        @Nullable
        Heightmap[] fcg$heightmapsAfter(ChunkStatus status);
    }

    private SurfaceWrites() {
    }

    public static BlockState get(ChunkAccess chunk, BlockPos pos) {
        if (chunk.getClass() != ProtoChunk.class) return chunk.getBlockState(pos);
        final FastChunk fast = (FastChunk) chunk;
        final int y = pos.getY();
        if (y < fast.fcg$minY() || y >= fast.fcg$maxY()) return Blocks.VOID_AIR.defaultBlockState();
        final LevelChunkSection section = chunk.getSection((y >> 4) - fast.fcg$minSection());
        return section.hasOnlyAir() ? Blocks.AIR.defaultBlockState() : section.getBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15);
    }

    public static BlockState set(ChunkAccess chunk, BlockPos pos, BlockState state, boolean moved) {
        if (chunk.getClass() == ProtoChunk.class) {
            final ChunkStatus status = chunk.getStatus();
            if (!status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                final FastChunk fast = (FastChunk) chunk;
                final Heightmap[] heightmaps = fast.fcg$heightmapsAfter(status);
                if (heightmaps != null) {
                    final int y = pos.getY();
                    if (y < fast.fcg$minY() || y >= fast.fcg$maxY()) return Blocks.VOID_AIR.defaultBlockState();
                    final LevelChunkSection section = chunk.getSection((y >> 4) - fast.fcg$minSection());
                    if (section.hasOnlyAir() && state.is(Blocks.AIR)) return state;
                    final int x = pos.getX() & 15;
                    final int z = pos.getZ() & 15;
                    final BlockState previous = section.setBlockState(x, y & 15, z, state, false);
                    for (Heightmap heightmap : heightmaps) heightmap.update(x, y, z, state);
                    return previous;
                }
            }
        }
        return chunk.setBlockState(pos, state, moved);
    }

}
