package com.misanthropy.fastchunkgen.opts.allocs;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;

public class Config {

    public static final boolean overrideModernFixWorldGenAllocations = new ConfigSystem.ConfigAccessor()
            .key("generalOptimizations.overrideModernFixWorldGenAllocations")
            .comment("Apply world generation allocation patches ahead of ModernFix.")
            .getBoolean(true, true);

    public static void init() {
    }
}
