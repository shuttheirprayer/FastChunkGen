package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class PointNoise implements DensityFunction.SimpleFunction {

    private final DensityFunction inner;
    private int x = Integer.MIN_VALUE, y, z;
    private double value;
    private int hits;

    PointNoise(DensityFunction inner) {
        this.inner = inner;
    }

    public DensityFunction inner() {
        return this.inner;
    }

    @Override
    public double compute(FunctionContext context) {
        final int x = context.blockX(), y = context.blockY(), z = context.blockZ();
        if (x == this.x && y == this.y && z == this.z) {
            if (ShortcutVerify.ENABLED) {
                ShortcutVerify.COLUMN_NOISE.record(this.value == this.inner.compute(context), "point " + x + "," + y + "," + z);
            } else if (ShortcutVerify.due(++this.hits)) {
                final double actual = this.inner.compute(context);
                if (Double.doubleToRawLongBits(actual) != Double.doubleToRawLongBits(this.value)) {
                    ShortcutVerify.COLUMN_NOISE.broke("point " + x + "," + y + "," + z);
                    this.value = actual;
                    return actual;
                }
            }
            return this.value;
        }
        final double value = this.inner.compute(context);
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = value;
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
