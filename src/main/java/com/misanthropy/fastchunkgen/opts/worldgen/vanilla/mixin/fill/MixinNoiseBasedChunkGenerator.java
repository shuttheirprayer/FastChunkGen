package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.CellBounds;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = NoiseBasedChunkGenerator.class, priority = 999)
public abstract class MixinNoiseBasedChunkGenerator {

    @Shadow @Final private static BlockState AIR;
    @Shadow @Final private Holder<NoiseGeneratorSettings> settings;

    @Shadow protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState randomState);

    @Shadow protected abstract BlockState debugPreliminarySurfaceLevel(NoiseChunk noiseChunk, int x, int y, int z, BlockState state);

    /**
     * @author Misanthropy
     * @reason skip cells proven all air or all solid etc
     */
    @Overwrite
    private ChunkAccess doFill(Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess chunk, int minCellY, int cellCountY) {
        final NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(c -> this.createNoiseChunk(c, structureManager, blender, randomState));
        final CellBounds.Cells cells = (CellBounds.Cells) noiseChunk;
        final Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        final long[] oceanFloorDone = new long[4];
        final long[] worldSurfaceDone = new long[4];
        final ChunkPos chunkPos = chunk.getPos();
        final int minX = chunkPos.getMinBlockX();
        final int minZ = chunkPos.getMinBlockZ();
        final boolean voidTerrain = SharedConstants.debugVoidTerrain(chunkPos);
        final BlockState defaultBlock = this.settings.value().defaultBlock();
        final Aquifer aquifer = noiseChunk.aquifer();
        cells.fcg$beginCellSorting(defaultBlock != AIR && !voidTerrain);
        noiseChunk.initializeForFirstCellX();
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        final int cellWidth = cells.fcg$cellWidth();
        final int cellHeight = cells.fcg$cellHeight();
        final int cellCountXZ = 16 / cellWidth;

        for (int cellX = 0; cellX < cellCountXZ; ++cellX) {
            noiseChunk.advanceCellX(cellX);

            for (int cellZ = 0; cellZ < cellCountXZ; ++cellZ) {
                int sectionIndex = chunk.getSectionsCount() - 1;
                LevelChunkSection section = chunk.getSection(sectionIndex);

                for (int cellY = cellCountY - 1; cellY >= 0; --cellY) {
                    noiseChunk.selectCellYZ(cellY, cellZ);
                    final int mode = cells.fcg$cellMode();
                    if (mode == CellBounds.AIR) continue;

                    for (int yInCell = cellHeight - 1; yInCell >= 0; --yInCell) {
                        final int y = (minCellY + cellY) * cellHeight + yInCell;
                        final int localY = y & 15;
                        final int index = chunk.getSectionIndex(y);
                        if (sectionIndex != index) {
                            sectionIndex = index;
                            section = chunk.getSection(index);
                        }

                        if (mode == CellBounds.SOLID) {
                            for (int xInCell = 0; xInCell < cellWidth; ++xInCell) {
                                final int x = minX + cellX * cellWidth + xInCell;
                                for (int zInCell = 0; zInCell < cellWidth; ++zInCell) {
                                    final int z = minZ + cellZ * cellWidth + zInCell;
                                    final BlockState state = this.debugPreliminarySurfaceLevel(noiseChunk, x, y, z, defaultBlock);
                                    if (state != AIR) {
                                        section.setBlockState(x & 15, localY, z & 15, state, false);
                                        fcg$updateHeightmap(oceanFloor, oceanFloorDone, x & 15, y, z & 15, state);
                                        fcg$updateHeightmap(worldSurface, worldSurfaceDone, x & 15, y, z & 15, state);
                                    }
                                }
                            }
                            continue;
                        }

                        noiseChunk.updateForY(y, (double) yInCell / (double) cellHeight);
                        for (int xInCell = 0; xInCell < cellWidth; ++xInCell) {
                            final int x = minX + cellX * cellWidth + xInCell;
                            final int localX = x & 15;
                            noiseChunk.updateForX(x, (double) xInCell / (double) cellWidth);
                            for (int zInCell = 0; zInCell < cellWidth; ++zInCell) {
                                final int z = minZ + cellZ * cellWidth + zInCell;
                                final int localZ = z & 15;
                                noiseChunk.updateForZ(z, (double) zInCell / (double) cellWidth);
                                BlockState state = cells.fcg$interpolatedState();
                                if (state == null) state = defaultBlock;
                                state = this.debugPreliminarySurfaceLevel(noiseChunk, x, y, z, state);
                                if (state != AIR && !voidTerrain) {
                                    section.setBlockState(localX, localY, localZ, state, false);
                                    fcg$updateHeightmap(oceanFloor, oceanFloorDone, localX, y, localZ, state);
                                    fcg$updateHeightmap(worldSurface, worldSurfaceDone, localX, y, localZ, state);
                                    if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                                        mutable.set(x, y, z);
                                        chunk.markPosForPostprocessing(mutable);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        noiseChunk.stopInterpolation();
        return chunk;
    }

    @Unique
    private static void fcg$updateHeightmap(Heightmap heightmap, long[] done, int x, int y, int z, BlockState state) {
        final int column = x | z << 4;
        final long bit = 1L << column;
        if ((done[column >> 6] & bit) == 0 && heightmap.update(x, y, z, state)) done[column >> 6] |= bit;
    }

}
