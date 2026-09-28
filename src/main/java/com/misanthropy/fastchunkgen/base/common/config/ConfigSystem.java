package com.misanthropy.fastchunkgen.base.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.misanthropy.fastchunkgen.base.common.util.BooleanUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import com.misanthropy.fastchunkgen.base.common.util.ModUtil;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Supplier;

public class ConfigSystem {

    public static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Config System");

    private static final long CURRENT_CONFIG_VERSION = 3;

    private static final Supplier<CommentedFileConfig> configSupplier =
            () -> CommentedFileConfig.builder(FMLPaths.CONFIGDIR.get().resolve("fastchunkgen.toml"))
                    .preserveInsertionOrder()
                    .sync()
                    .build();

    private static final CommentedFileConfig CONFIG;

    private static final HashSet<String> visitedConfig = new HashSet<>();

    static {
        CommentedFileConfig config = configSupplier.get();
        try {
            config.load();
        } catch (Throwable t) {
            config = configSupplier.get();
            config.save();
        }

        Updaters.update(config);
        if (config.getInt("version") != CURRENT_CONFIG_VERSION) {
            config.clear();
            LOGGER.warn("Config version mismatch, resetting config");
            config.set("version", CURRENT_CONFIG_VERSION);
        }

        visitedConfig.add("version");

        CONFIG = config;
    }

    public static boolean readBoolean(String key, boolean fallback) {
        final Object value = CONFIG.get(key);
        if (value instanceof Boolean b) return b;
        final String raw = readString(key, null);
        if (raw != null) {
            if (raw.equalsIgnoreCase("true")) return true;
            if (raw.equalsIgnoreCase("false")) return false;
        }
        return fallback;
    }

    public static long readLong(String key, long fallback) {
        final Object value = CONFIG.get(key);
        if (value instanceof Number n) return n.longValue();
        final String raw = readString(key, null);
        if (raw != null) {
            try {
                return Long.parseLong(raw.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    public static String readString(String key, String fallback) {
        final Object value = CONFIG.get(key);
        if (value == null) return fallback;
        if (value instanceof Enum<?> e) return e.name();
        final String raw = String.valueOf(value);
        return raw.equals("default") ? fallback : raw;
    }

    public static void write(String key, Object value) {
        CONFIG.set(key, value);
    }

    public static void saveToDisk() {
        CONFIG.save();
    }

    private static final int MINIMUM_EXPECTED_KEYS = 24;

    public static void flushConfig() {
        if (visitedConfig.size() < MINIMUM_EXPECTED_KEYS) {
            LOGGER.warn("Only {} config keys were registered, skipping cleanup to avoid discarding settings", visitedConfig.size());
        } else {
            purgeUnusedRecursively("", CONFIG);
        }
        CONFIG.save();
    }

    private static void purgeUnusedRecursively(String prefix, CommentedConfig config) {
        for (Iterator<? extends CommentedConfig.Entry> iterator = config.entrySet().iterator(); iterator.hasNext(); ) {
            CommentedConfig.Entry entry = iterator.next();
            final String key = prefix + "." + entry.getKey();
            if (entry.getValue() instanceof CommentedConfig child) {
                purgeUnusedRecursively(key, child);
                if (child.isEmpty()) {
                    LOGGER.info("Removing config entry {} because it is not used", key);
                    iterator.remove();
                }
            } else if (!visitedConfig.contains(key.substring(".".length()))) {
                LOGGER.info("Removing config entry {} because it is not used", key);
                iterator.remove();
            }
        }
    }

    public static class ConfigAccessor {

        private static final String propertyPrefix = "fastchunkgen.config.override.";

        private final StringBuilder incompatibilityReason = new StringBuilder();

        private String key;
        private String comment;
        private boolean incompatibilityDetected;

        public ConfigAccessor key(String key) {
            this.key = key;
            markVisited();
            return this;
        }

        public ConfigAccessor comment(String comment) {
            this.comment = comment;
            return this;
        }

        public ConfigAccessor incompatibleMod(String modId, String predicate) {
            // SJhub start - rewrite find incompatible mod
            try {
                if (!ModUtil.isModLoaded(modId)) {
                    return this;
                }

                VersionRange versionRange = VersionRange.createFromVersionSpec(predicate);
                ArtifactVersion version = ModUtil.getModVersion(modId);

                if (versionRange.containsVersion(version)) {
                    String reason = String.format("Incompatible with %s@%s (%s) (defined in fastchunkgen)", modId, version.toString(), predicate);
                    disableConfigWithReason(reason);
                }

            } catch (Throwable t) {
                throw new IllegalArgumentException(t);
            }

            return this;
            // SJhub end - rewrite find incompatible mod
        }

        public long getLong(long def, long incompatibleDef, LongChecks... checks) {
            findModDefinedIncompatibility();
            final String systemPropertyOverride = getSystemPropertyOverride();
            final Object configured = systemPropertyOverride != null ? systemPropertyOverride : CONFIG.get(this.key);
            boolean isDefaultValue;
            if (configured != null) {
                if (String.valueOf(configured).equals("default")) { // default placeholder
                    isDefaultValue = true;
                } else if (!(configured instanceof Number)) { // try to fix config
                    try {
                        CONFIG.set(this.key, Long.valueOf(String.valueOf(configured)));
                        isDefaultValue = false;
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid configured value: {} -> {}", this.key, configured);
                        CONFIG.remove(this.key);
                        isDefaultValue = true;
                    }
                } else {
                    isDefaultValue = false;
                }
            } else {
                isDefaultValue = true;
            }
            generateDefaultEntry(def, incompatibleDef);

            if (this.incompatibilityDetected) return incompatibleDef;
            long configLong = isDefaultValue ? def : CONFIG.getLong(this.key);
            if (checkConfig(configLong, checks)) {
                return configLong;
            } else {
                CONFIG.remove(this.key);
                generateDefaultEntry(def, incompatibleDef);
                return def;
            }
        }

        private boolean checkConfig(long value, LongChecks... checks) {
            for (LongChecks check : checks) {
                if (!check.test(value)) return false;
            }
            return true;
        }

        public boolean getBoolean(boolean def, boolean incompatibleDef) {
            findModDefinedIncompatibility();
            final String systemPropertyOverride = getSystemPropertyOverride();
            final Object configured = systemPropertyOverride != null ? systemPropertyOverride : CONFIG.get(this.key);
            boolean isDefaultValue;
            if (configured != null) {
                if (String.valueOf(configured).equals("default")) { // default placeholder
                    isDefaultValue = true;
                } else if (!(configured instanceof Boolean)) { // try to fix config
                    try {
                        CONFIG.set(this.key, BooleanUtils.parseBoolean(String.valueOf(configured)));
                        isDefaultValue = false;
                    } catch (BooleanUtils.BooleanFormatException e) {
                        LOGGER.warn("Invalid configured value: {} -> {}", this.key, configured);
                        CONFIG.remove(this.key);
                        isDefaultValue = true;
                    }
                } else {
                    isDefaultValue = false;
                }
            } else {
                isDefaultValue = true;
            }
            generateDefaultEntry(def, incompatibleDef);

            return this.incompatibilityDetected ? incompatibleDef : (isDefaultValue ? def : CONFIG.get(this.key));
        }

        public <T extends Enum<T>> T getEnum(Class<T> enumClass, T def, T incompatibleDef) {
            findModDefinedIncompatibility();
            final String systemPropertyOverride = getSystemPropertyOverride();
            final Object configured = systemPropertyOverride != null ? systemPropertyOverride : CONFIG.get(this.key);
            boolean isDefaultValue;
            if (configured != null) {
                if (String.valueOf(configured).equals("default")) {
                    isDefaultValue = true;
                } else {
                    try {
                        CONFIG.set(this.key, Enum.valueOf(enumClass, String.valueOf(configured)));
                        isDefaultValue = false;
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid configured value: {} -> {}", this.key, configured);
                        CONFIG.remove(this.key);
                        isDefaultValue = true;
                    }
                }
            } else {
                isDefaultValue = true;
            }
            generateDefaultEntry(def, incompatibleDef);

            return this.incompatibilityDetected ? incompatibleDef : (isDefaultValue ? def : CONFIG.getEnum(this.key, enumClass));
        }

        public String getString(String def, String incompatibleDef) {
            findModDefinedIncompatibility();
            final String systemPropertyOverride = getSystemPropertyOverride();
            final Object configured = systemPropertyOverride != null ? systemPropertyOverride : CONFIG.get(this.key);
            boolean isDefaultValue;
            if (configured != null) {
                if (String.valueOf(configured).equals("default")) { // default placeholder
                    isDefaultValue = true;
                } else if (!(configured instanceof String)) { // try to fix config
                    CONFIG.set(this.key, String.valueOf(configured));
                    isDefaultValue = false;
                } else {
                    isDefaultValue = false;
                }
            } else {
                isDefaultValue = true;
            }
            generateDefaultEntry(def, incompatibleDef);

            return this.incompatibilityDetected ? incompatibleDef : (isDefaultValue ? def : CONFIG.get(this.key));
        }

        private void findModDefinedIncompatibility() {
            for (IModInfo modInfo : ModUtil.getModInfoList()) {
                Object incompatibilitiesValue = modInfo.getModProperties().get("fastchunkgen:incompatibleConfig");
                if (incompatibilitiesValue instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> incompatibilities = (java.util.List<String>) incompatibilitiesValue;
                    for (String value : incompatibilities) {
                        if (value.equals(this.key)) {
                            final String reason = String.format("Incompatible with %s@%s (defined in %s)",
                                    modInfo.getModId(), modInfo.getVersion().toString(), modInfo.getModId());
                            disableConfigWithReason(reason);
                        }
                    }
                }
            }

        }

        private String getSystemPropertyOverride() {
            final String property = System.getProperty(propertyPrefix + this.key);
            if (property != null) LOGGER.info("Setting {} to {} (defined in system property)", this.key, property);
            return property;
        }

        private void disableConfigWithReason(String reason) {
            incompatibilityReason.append("\n ").append(reason);
            if (!Boolean.getBoolean("com.misanthropy.fastchunkgen.base.config.ignoreIncompatibility")) {
                LOGGER.info("Disabling config {}: {}", this.key, reason);
                this.incompatibilityDetected = true;
            } else {
                LOGGER.info("Compatibility issues ignored by system property: {}: {}", this.key, reason);
            }
        }

        private void generateDefaultEntry(Object def, Object incompatibleDef) {
            if (!CONFIG.contains(this.key)) {
                CONFIG.set(this.key, "default");
            }
            final String comment = String.format(" (Default: %s) %s", def, this.comment.replace("\n", "\n "));
            if (this.incompatibilityDetected) {
                CONFIG.setComment(this.key, String.format("%s\n Set to %s for the following reasons: %s ", comment, incompatibleDef, this.incompatibilityReason));
            } else {
                CONFIG.setComment(this.key, comment);
            }
        }

        private void markVisited() {
            visitedConfig.add(this.key);
        }
    }

    public enum LongChecks {
        THREAD_COUNT() {
            @Override
            public boolean test(long value) {
                return value >= 1 && value <= 0x7fff;
            }
        },
        VIEW_DISTANCE() {
            @Override
            public boolean test(long value) {
                return value >= 32 && value <= 125;
            }
        },
        POSITIVE_VALUES_ONLY() {
            @Override
            public boolean test(long value) {
                return value >= 1;
            }
        };

        public abstract boolean test(long value);

    }

}
