package com.misanthropy.fastchunkgen.opts.chunkio;

import com.misanthropy.fastchunkgen.base.common.ModuleMixinPlugin;
import com.misanthropy.fastchunkgen.opts.chunkio.common.Config;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.startsWith("com.misanthropy.fastchunkgen.opts.chunkio.mixin.compression.modify_default_chunk_compression"))
            return Config.chunkStreamVersion != -1;

        if (mixinClassName.startsWith("com.misanthropy.fastchunkgen.opts.chunkio.mixin.skip_missing_external_chunk_delete."))
            return Config.skipMissingExternalChunkDelete;

        return true;
    }
}
