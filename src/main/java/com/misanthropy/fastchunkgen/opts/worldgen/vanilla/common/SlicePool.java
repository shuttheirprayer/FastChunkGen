package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import java.util.ArrayDeque;

public final class SlicePool {

    private static final int LIMIT = 96;
    private static final ThreadLocal<ArrayDeque<double[][]>> POOL = ThreadLocal.withInitial(ArrayDeque::new);

    private SlicePool() {
    }

    public static ArrayDeque<double[][]> local() {
        return POOL.get();
    }

    public static double[][] take(ArrayDeque<double[][]> pool, int rows, int columns) {
        double[][] slice = pool.pollFirst();
        return slice != null && slice.length == rows && slice[0].length == columns ? slice : null;
    }

    public static void give(ArrayDeque<double[][]> pool, double[][] slice) {
        if (slice != null && pool.size() < LIMIT) {
            pool.addFirst(slice);
        }
    }
}
