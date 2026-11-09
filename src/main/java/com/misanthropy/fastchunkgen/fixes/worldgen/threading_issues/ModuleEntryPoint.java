package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;
import com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.Config;

public class ModuleEntryPoint {

    private static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("fixes.worldGenThreadingIssues")
            .comment("Fix thread-safety issues in vanilla world generation. Required by threadedWorldGen.")
            .getBoolean(true, false);


    static {
        Config.init();
    }

}
