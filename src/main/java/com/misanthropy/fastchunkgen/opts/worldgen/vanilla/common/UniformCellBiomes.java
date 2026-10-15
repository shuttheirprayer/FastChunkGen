package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

import java.util.function.Function;

public final class UniformCellBiomes implements Function<BlockPos, Holder<Biome>> {

    private static final Object MIXED = new Object();

    private final BiomeManager.NoiseBiomeSource source;
    private final Function<BlockPos, Holder<Biome>> fallback;
    private final long[] keys = new long[64];
    private final Object[] values = new Object[64];

    public UniformCellBiomes(BiomeManager.NoiseBiomeSource source, Function<BlockPos, Holder<Biome>> fallback) {
        this.source = source;
        this.fallback = fallback;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Holder<Biome> apply(BlockPos pos) {
        final int qx = (pos.getX() - 2) >> 2;
        final int qy = (pos.getY() - 2) >> 2;
        final int qz = (pos.getZ() - 2) >> 2;
        final long key = BlockPos.asLong(qx, qy, qz);
        final int slot = (qx & 3) | (qz & 3) << 2 | (qy & 3) << 4;
        Object value = this.values[slot];
        if (value == null || this.keys[slot] != key) {
            value = this.uniformBiome(qx, qy, qz);
            this.keys[slot] = key;
            this.values[slot] = value;
        }
        return value != MIXED ? (Holder<Biome>) value : this.fallback.apply(pos);
    }

    private Object uniformBiome(int qx, int qy, int qz) {
        final Holder<Biome> first = this.source.getNoiseBiome(qx, qy, qz);
        for (int corner = 1; corner < 8; corner++) {
            if (this.source.getNoiseBiome(qx + (corner >> 2 & 1), qy + (corner >> 1 & 1), qz + (corner & 1)) != first) {
                return MIXED;
            }
        }
        return first;
    }

}
