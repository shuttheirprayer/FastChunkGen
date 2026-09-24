package com.misanthropy.fastchunkgen.threading.worldgen.common;

import com.misanthropy.fastchunkgen.base.common.compat.ModCompat;
import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;

public class Config {

    public static final boolean allowThreadedFeatures = new ConfigSystem.ConfigAccessor()
            .key("threadedWorldGen.allowThreadedFeatures")
            .comment("Generate features (trees, ores, structures) in parallel.")
            .getBoolean(true, false);

    public static final boolean reduceLockRadius = new ConfigSystem.ConfigAccessor()
            .key("threadedWorldGen.reduceLockRadius")
            .comment("Lock fewer neighbouring chunks per generation step. More parallelism.")
            .getBoolean(true, false);

    public static final boolean debugReducedLockRadius = reduceLockRadius
            && Boolean.getBoolean("com.misanthropy.fastchunkgen.common.threading.worldgen.debug.DebugReducedLockRadius");

    public static final boolean asyncScheduling = new ConfigSystem.ConfigAccessor()
            .key("threadedWorldGen.asyncScheduling")
            .comment("Schedule generation tasks off the server thread.")
            .getBoolean(true, false)
            && !ModCompat.isModernFixFeatureEnabled("mixin.perf.release_protochunks");

    // If you are having issues with pre-generating. Disable this!
    public static final boolean allowGenerationCancellation = new ConfigSystem.ConfigAccessor()
            .key("threadedWorldGen.allowGenerationCancellation")
            .comment("Abort an in-progress generation step when the chunk is no longer requested. Set to false if a pregenerator reports skipped chunks.")
            .getBoolean(true, false);

    public static void init() {
    }

}
