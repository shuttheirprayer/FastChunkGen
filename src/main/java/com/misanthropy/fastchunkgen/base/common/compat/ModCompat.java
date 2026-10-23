package com.misanthropy.fastchunkgen.base.common.compat;

import com.misanthropy.fastchunkgen.base.common.util.ModUtil;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class ModCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Compat");

    private static final String[] LITHIUM_FAMILY = {"lithium", "radium", "canary"};

    private static Boolean lithiumFamily;

    private static Properties modernFixMixins;

    public static final String MODERNFIX_RELEASE_PROTOCHUNKS = "mixin.perf.release_protochunks";

    private static boolean modernFixProtoChunkReleaseDisabled;

    public static boolean isLithiumFamilyPresent() {
        if (lithiumFamily == null) {
            boolean present = false;
            for (String id : LITHIUM_FAMILY) {
                if (ModUtil.isModLoaded(id)) {
                    present = true;
                    break;
                }
            }
            lithiumFamily = present;
        }
        return lithiumFamily;
    }

    public static boolean isModernFixPresent() {
        return ModUtil.isModLoaded("modernfix");
    }

    public static boolean isCanaryPresent() {
        return ModUtil.isModLoaded("canary");
    }

    private static Properties modernFixMixins() {
        if (modernFixMixins == null) {
            final Properties properties = new Properties();
            try {
                final Path path = FMLPaths.CONFIGDIR.get().resolve("modernfix-mixins.properties");
                if (Files.isRegularFile(path)) {
                    try (InputStream in = Files.newInputStream(path)) {
                        properties.load(in);
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("Could not read modernfix-mixins.properties, assuming ModernFix defaults", t);
            }
            modernFixMixins = properties;
        }
        return modernFixMixins;
    }

    public static void disableModernFixProtoChunkRelease() {
        if (!isModernFixPresent() || !isModernFixFeatureEnabled(MODERNFIX_RELEASE_PROTOCHUNKS)) return;
        try {
            System.setProperty("modernfix.config." + MODERNFIX_RELEASE_PROTOCHUNKS, "false");
            final Class<?> plugin = Class.forName("org.embeddedt.modernfix.core.ModernFixMixinPlugin", false, ModCompat.class.getClassLoader());
            final Object instance = plugin.getField("instance").get(null);
            if (instance != null) {
                final Object config = plugin.getField("config").get(instance);
                final Object option = ((Map<?, ?>) config.getClass().getMethod("getOptionMap").invoke(config)).get(MODERNFIX_RELEASE_PROTOCHUNKS);
                option.getClass().getMethod("addModOverride", Object.class, String.class).invoke(option, false, "fastchunkgen");
            }
            modernFixProtoChunkReleaseDisabled = true;
        } catch (Throwable t) {
            LOGGER.warn("Could not disable ModernFix {}", MODERNFIX_RELEASE_PROTOCHUNKS, t);
        }
    }

    public static boolean isModernFixProtoChunkReleaseDisabled() {
        return modernFixProtoChunkReleaseDisabled;
    }

    public static boolean isModernFixFeatureEnabled(String key) {
        if (!isModernFixPresent()) return false;
        if (modernFixProtoChunkReleaseDisabled && key.equals(MODERNFIX_RELEASE_PROTOCHUNKS)) return false;
        final String value = modernFixMixins().getProperty(key);
        return value == null || !value.equalsIgnoreCase("false");
    }
}
