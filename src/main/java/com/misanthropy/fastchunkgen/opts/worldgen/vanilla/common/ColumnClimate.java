package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ColumnClimate implements DensityFunction {

    private static final Map<DensityFunction, Boolean> Y_FREE = Collections.synchronizedMap(new WeakHashMap<>());

    private final DensityFunction wrapped;
    private final Thread owner = Thread.currentThread();
    private final long[] keys = new long[16];
    private final double[] values = new double[16];

    private ColumnClimate(DensityFunction wrapped) {
        this.wrapped = wrapped;
        Arrays.fill(this.keys, Long.MIN_VALUE);
    }

    public static Climate.Sampler wrap(Climate.Sampler sampler, NoiseRouter router) {
        return new Climate.Sampler(
                cache(sampler.temperature(), router.temperature()),
                cache(sampler.humidity(), router.vegetation()),
                cache(sampler.continentalness(), router.continents()),
                cache(sampler.erosion(), router.erosion()),
                cache(sampler.depth(), router.depth()),
                cache(sampler.weirdness(), router.ridges()),
                sampler.spawnTarget());
    }

    private static DensityFunction cache(DensityFunction wrapped, DensityFunction unwrapped) {
        Boolean yFree = Y_FREE.get(unwrapped);
        if (yFree == null) {
            yFree = SharedNoise.isYFree(wrapped);
            Y_FREE.put(unwrapped, yFree);
        }
        return yFree ? new ColumnClimate(wrapped) : wrapped;
    }

    @Override
    public double compute(FunctionContext context) {
        if (Thread.currentThread() != this.owner) return this.wrapped.compute(context);
        final int x = context.blockX();
        final int z = context.blockZ();
        final long key = (long) x << 32 | (z & 0xFFFFFFFFL);
        final int slot = (x >> 2 & 3) << 2 | (z >> 2 & 3);
        if (this.keys[slot] == key) {
            if (ShortcutVerify.ENABLED) {
                ShortcutVerify.COLUMN.record(Double.doubleToRawLongBits(this.values[slot]) == Double.doubleToRawLongBits(this.wrapped.compute(context)), x + "," + context.blockY() + "," + z);
            }
            return this.values[slot];
        }
        final double value = this.wrapped.compute(context);
        this.keys[slot] = key;
        this.values[slot] = value;
        return value;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        this.wrapped.fillArray(array, provider);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return this.wrapped.mapAll(visitor);
    }

    @Override
    public double minValue() {
        return this.wrapped.minValue();
    }

    @Override
    public double maxValue() {
        return this.wrapped.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return this.wrapped.codec();
    }
}
