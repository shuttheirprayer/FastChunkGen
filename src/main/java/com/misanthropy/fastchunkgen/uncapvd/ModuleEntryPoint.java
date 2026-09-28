package com.misanthropy.fastchunkgen.uncapvd;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;

public class ModuleEntryPoint {

    public static final int maxViewDistance = (int) new ConfigSystem.ConfigAccessor()
            .key("maxViewDistance")
            .comment("Highest view/render distance allowed (vanilla caps at 32, max 125). Higher values need more memory.")
            .getLong(32, 32, ConfigSystem.LongChecks.VIEW_DISTANCE);

    private static final boolean enabled = maxViewDistance > 32;

}
