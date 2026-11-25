package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Arrays;

public final class ColumnNoise implements DensityFunction.SimpleFunction {

    private final DensityFunction inner;
    private final int minX, minZ, size;
    private double[] values;

    ColumnNoise(DensityFunction inner, int minX, int minZ, int size) {
        this.inner = inner;
        this.minX = minX;
        this.minZ = minZ;
        this.size = size;
    }

    public DensityFunction inner() {
        return this.inner;
    }

    @Override
    public double compute(FunctionContext context) {
        final int x = context.blockX() - this.minX;
        final int z = context.blockZ() - this.minZ;
        final int size = this.size;
        if (x < 0 || z < 0 || x >= size || z >= size) return this.inner.compute(context);
        double[] values = this.values;
        if (values == null) {
            values = new double[size * size];
            Arrays.fill(values, Double.NaN);
            this.values = values;
        }
        final int index = x * size + z;
        double value = values[index];
        if (Double.isNaN(value)) {
            value = this.inner.compute(context);
            values[index] = value;
        } else if (ShortcutVerify.ENABLED) {
            ShortcutVerify.COLUMN_NOISE.record(value == this.inner.compute(context), context.blockX() + "," + context.blockY() + "," + context.blockZ());
        }
        return value;
    }

    @Override
    public double minValue() {
        return this.inner.minValue();
    }

    @Override
    public double maxValue() {
        return this.inner.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return this.inner.codec();
    }
}
