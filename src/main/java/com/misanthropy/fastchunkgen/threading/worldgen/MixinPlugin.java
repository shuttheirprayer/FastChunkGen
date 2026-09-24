package com.misanthropy.fastchunkgen.threading.worldgen;

import com.misanthropy.fastchunkgen.base.common.ModuleMixinPlugin;
import com.misanthropy.fastchunkgen.threading.worldgen.common.Config;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.equals("com.misanthropy.fastchunkgen.threading.worldgen.mixin.MixinChunkRegion"))
            return Config.debugReducedLockRadius;

        return true;
    }
}
