package com.misanthropy.fastchunkgen.opts.scheduling;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;
import com.misanthropy.fastchunkgen.opts.scheduling.common.Config;

public class ModuleEntryPoint {

    private static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("generalOptimizations.optimizeScheduling")
            .comment("Optimize chunk task scheduling.")
            .getBoolean(true, false);


    static {
        Config.init();
    }

}
