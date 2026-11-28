package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;

public class Config {

    public static final boolean enforceSafeWorldRandomAccess = new ConfigSystem.ConfigAccessor()
            .key("fixes.enforceSafeWorldRandomAccess")
            .comment("Crash instead of warn. Mainly for debugging.")
            .getBoolean(false, false);

    public static final boolean releaseFailedDataFixerRewrites = new ConfigSystem.ConfigAccessor()
            .key("fixes.releaseFailedDataFixerRewrites")
            .comment("If a thread fails inside a DataFixerUpper type rewrite, release the threads waiting on it with the same error instead of leaving them blocked forever.")
            .getBoolean(true, false);

    public static void init() {
    }

}
