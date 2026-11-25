package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayDeque;

public final class SlicePool {

    private static final int LIMIT = 96;
    private static final ThreadLocal<SlicePool> POOL = ThreadLocal.withInitial(SlicePool::new);

    private final Int2ObjectOpenHashMap<ArrayDeque<double[][]>> shapes = new Int2ObjectOpenHashMap<>();

    private SlicePool() {
    }

    public static SlicePool local() {
        return POOL.get();
    }

    public double[][] take(int rows, int columns) {
        ArrayDeque<double[][]> pool = this.shapes.get(rows << 16 | columns);
        return pool == null ? null : pool.pollFirst();
    }

    public void give(double[][] slice) {
        if (slice == null) return;
        int shape = slice.length << 16 | slice[0].length;
        ArrayDeque<double[][]> pool = this.shapes.get(shape);
        if (pool == null) {
            this.shapes.put(shape, pool = new ArrayDeque<>());
        }
        if (pool.size() < LIMIT) {
            pool.addFirst(slice);
        }
    }
}
