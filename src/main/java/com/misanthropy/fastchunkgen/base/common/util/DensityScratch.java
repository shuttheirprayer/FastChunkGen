package com.misanthropy.fastchunkgen.base.common.util;

public final class DensityScratch {
    private static final ThreadLocal<DensityScratch> FALLBACK = ThreadLocal.withInitial(DensityScratch::new);

    public final double[][] scratch = new double[16][];
    public int depth;
    public Object[] splinePoints = new Object[8];
    public int splineDepth;

    public static DensityScratch fallback() {
        return FALLBACK.get();
    }
}
