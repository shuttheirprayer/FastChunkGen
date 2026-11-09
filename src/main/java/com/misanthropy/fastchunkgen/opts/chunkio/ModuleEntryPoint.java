package com.misanthropy.fastchunkgen.opts.chunkio;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;
import com.misanthropy.fastchunkgen.opts.chunkio.common.Config;

public class ModuleEntryPoint {

    private static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.optimizations")
            .comment("Enable chunk IO optimizations.")
            .getBoolean(true, false);


    static {
        Config.init();
    }

}
