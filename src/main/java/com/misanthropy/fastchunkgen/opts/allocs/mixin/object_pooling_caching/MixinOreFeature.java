package com.misanthropy.fastchunkgen.opts.allocs.mixin.object_pooling_caching;

import com.misanthropy.fastchunkgen.opts.allocs.common.ObjectCachingUtils;
import com.misanthropy.fastchunkgen.opts.allocs.common.OreSectionAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.BitSet;
import java.util.List;

@Mixin(value = OreFeature.class, priority = 900)
public class MixinOreFeature {

    /**
     * @author misanthropy
     * @reason reuse per-thread scratch instead of allocating per vein
     */
    @Overwrite
    protected boolean doPlace(WorldGenLevel level, RandomSource random, OreConfiguration config, double minX, double maxX, double minZ, double maxZ, double minY, double maxY, int x, int y, int z, int width, int height) {
        int placed = 0;
        BitSet visited = ObjectCachingUtils.getCachedOrNewBitSet(width * height * width);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int size = config.size;
        try (OreSectionAccess sections = OreSectionAccess.open(level)) {
            double[] blobs = sections.blobs(size * 4);

            for (int i = 0; i < size; ++i) {
                float progress = (float) i / (float) size;
                double bx = Mth.lerp(progress, minX, maxX);
                double by = Mth.lerp(progress, minY, maxY);
                double bz = Mth.lerp(progress, minZ, maxZ);
                double scale = random.nextDouble() * (double) size / 16.0D;
                double radius = ((double) (Mth.sin((float) Math.PI * progress) + 1.0F) * scale + 1.0D) / 2.0D;
                blobs[i * 4] = bx;
                blobs[i * 4 + 1] = by;
                blobs[i * 4 + 2] = bz;
                blobs[i * 4 + 3] = radius;
            }

            for (int i = 0; i < size - 1; ++i) {
                if (!(blobs[i * 4 + 3] <= 0.0D)) {
                    for (int j = i + 1; j < size; ++j) {
                        if (!(blobs[j * 4 + 3] <= 0.0D)) {
                            double dx = blobs[i * 4] - blobs[j * 4];
                            double dy = blobs[i * 4 + 1] - blobs[j * 4 + 1];
                            double dz = blobs[i * 4 + 2] - blobs[j * 4 + 2];
                            double dr = blobs[i * 4 + 3] - blobs[j * 4 + 3];
                            if (dr * dr > dx * dx + dy * dy + dz * dz) {
                                if (dr > 0.0D) {
                                    blobs[j * 4 + 3] = -1.0D;
                                } else {
                                    blobs[i * 4 + 3] = -1.0D;
                                }
                            }
                        }
                    }
                }
            }

            List<OreConfiguration.TargetBlockState> targets = config.targetStates;
            int targetCount = targets.size();
            for (int i = 0; i < size; ++i) {
                double radius = blobs[i * 4 + 3];
                if (!(radius < 0.0D)) {
                    double cx = blobs[i * 4];
                    double cy = blobs[i * 4 + 1];
                    double cz = blobs[i * 4 + 2];
                    int x0 = Math.max(Mth.floor(cx - radius), x);
                    int y0 = Math.max(Mth.floor(cy - radius), y);
                    int z0 = Math.max(Mth.floor(cz - radius), z);
                    int x1 = Math.max(Mth.floor(cx + radius), x0);
                    int y1 = Math.max(Mth.floor(cy + radius), y0);
                    int z1 = Math.max(Mth.floor(cz + radius), z0);
                    double[] depths = sections.depths(z1 - z0 + 1);
                    for (int pz = z0; pz <= z1; ++pz) {
                        double uz = ((double) pz + 0.5D - cz) / radius;
                        depths[pz - z0] = uz * uz;
                    }

                    for (int px = x0; px <= x1; ++px) {
                        double ux = ((double) px + 0.5D - cx) / radius;
                        if (ux * ux < 1.0D) {
                            for (int py = y0; py <= y1; ++py) {
                                double uy = ((double) py + 0.5D - cy) / radius;
                                double flat = ux * ux + uy * uy;
                                if (flat < 1.0D && !level.isOutsideBuildHeight(py)) {
                                    int row = px - x + (py - y) * width - z * width * height;
                                    for (int pz = z0; pz <= z1; ++pz) {
                                        if (flat + depths[pz - z0] < 1.0D) {
                                            int bit = row + pz * width * height;
                                            if (!visited.get(bit)) {
                                                visited.set(bit);
                                                pos.set(px, py, pz);
                                                if (level.ensureCanWrite(pos)) {
                                                    LevelChunkSection section = sections.getSection(pos);
                                                    if (section != null) {
                                                        int sx = SectionPos.sectionRelative(px);
                                                        int sy = SectionPos.sectionRelative(py);
                                                        int sz = SectionPos.sectionRelative(pz);
                                                        BlockState state = section.getBlockState(sx, sy, sz);

                                                        for (int t = 0; t < targetCount; ++t) {
                                                            OreConfiguration.TargetBlockState target = targets.get(t);
                                                            if (OreFeature.canPlaceOre(state, sections, random, config, target, pos)) {
                                                                section.setBlockState(sx, sy, sz, target.state, false);
                                                                ++placed;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return placed > 0;
    }

}
