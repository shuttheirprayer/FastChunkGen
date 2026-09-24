package com.misanthropy.fastchunkgen.opts.allocs;

import com.misanthropy.fastchunkgen.base.common.ModuleMixinPlugin;
import com.misanthropy.fastchunkgen.base.common.compat.ModCompat;
import com.misanthropy.fastchunkgen.base.common.util.ModUtil;

public class MixinPlugin extends ModuleMixinPlugin {

    private static final String PACKAGE = "com.misanthropy.fastchunkgen.opts.allocs.mixin.";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.equals(PACKAGE + "MixinNbtCompound") && ModUtil.isModLoaded("recoleta")) return false;

        if (mixinClassName.equals(PACKAGE + "MixinNbtCompound") || mixinClassName.equals(PACKAGE + "MixinNbtList")) {
            return !ModCompat.isLithiumFamilyPresent();
        }

        if (mixinClassName.startsWith(PACKAGE + "surfacebuilder.") || mixinClassName.startsWith(PACKAGE + "noise.")) {
            if (!Config.overrideModernFixWorldGenAllocations
                    && ModCompat.isModernFixFeatureEnabled("mixin.perf.worldgen_allocation")) {
                return false;
            }
            return !ModUtil.isModLoaded("quilted_fabric_api") && !ModUtil.isModLoaded("frozenlib");
        }

        return true;
    }
}
