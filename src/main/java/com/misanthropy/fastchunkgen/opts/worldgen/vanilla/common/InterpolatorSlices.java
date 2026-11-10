package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

public interface InterpolatorSlices {

    void fcg$acquireSlices(int cellCountY, int cellCountZ);

    void fcg$releaseSlices();

}
