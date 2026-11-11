package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues;

import com.misanthropy.fastchunkgen.base.common.ModuleMixinPlugin;
import com.misanthropy.fastchunkgen.base.common.util.ModUtil;
import com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.debug.SMAPPool;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.service.MixinService;

public class MixinPlugin extends ModuleMixinPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen WorldGen Fixes");
    private static final String LOST_CITIES_CACHE_MIXIN = "MixinLostCitiesBoundedCache";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;
        if (mixinClassName.endsWith(LOST_CITIES_CACHE_MIXIN)) return !ModUtil.isModLoaded("lc2h") && hasLegacyLostCitiesCache(targetClassName);
        return true;
    }

    private static boolean hasLegacyLostCitiesCache(String targetClassName) {
        try {
            final ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            return node.fields.stream().anyMatch(f -> f.name.equals("values") && f.desc.equals("Ljava/util/LinkedHashMap;"));
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        super.onLoad(mixinPackage);
        if (MixinEnvironment.getCurrentEnvironment().getActiveTransformer() instanceof IMixinTransformer transformer &&
            transformer.getExtensions() instanceof Extensions extensions) {
            extensions.add(new IExtension() {
                @Override
                public boolean checkActive(MixinEnvironment environment) {
                    return true;
                }

                @Override
                public void preApply(ITargetClassContext context) {

                }

                @Override
                public void postApply(ITargetClassContext context) {

                }

                @Override
                public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
                    SMAPPool.put(name, classNode);
                }
            });
        } else {
            LOGGER.warn("Failed to initialize SMAP parser for safe world random access, mod information for mixin injected methods will not be available");
        }
    }
}
