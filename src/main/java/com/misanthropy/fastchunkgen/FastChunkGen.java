package com.misanthropy.fastchunkgen;

import com.misanthropy.fastchunkgen.base.common.compat.ModCompat;
import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;
import com.misanthropy.fastchunkgen.rewrites.chunk_serializer.TheMod;
import com.misanthropy.fastchunkgen.threading.worldgen.common.BiomeSourceWarmup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod(FastChunkGen.MOD_ID)
public class FastChunkGen {

    public static final String MOD_ID = "fastchunkgen";

    public static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen");

    private static final List<String> MODULE_ENTRYPOINTS = List.of(
            "com.misanthropy.fastchunkgen.base.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.fixes.chunkio.threading_issues.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.fixes.general.threading_issues.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.fixes.worldgen.vanilla_bugs.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.opts.allocs.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.opts.chunk_access.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.opts.chunkio.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.opts.math.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.opts.scheduling.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.opts.worldgen.general.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.opts.worldgen.vanilla.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.rewrites.chunk_serializer.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.rewrites.chunkio.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.threading.chunkio.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.threading.lighting.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.threading.worldgen.ModuleEntryPoint",
            "com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.Config",
            "com.misanthropy.fastchunkgen.opts.allocs.Config",
            "com.misanthropy.fastchunkgen.opts.chunkio.common.Config",
            "com.misanthropy.fastchunkgen.opts.scheduling.common.Config",
            "com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.Config",
            "com.misanthropy.fastchunkgen.threading.chunkio.common.Config",
            "com.misanthropy.fastchunkgen.threading.worldgen.common.Config"
    );

    public FastChunkGen() {
        for (String name : MODULE_ENTRYPOINTS) {
            try {
                Class.forName(name, true, FastChunkGen.class.getClassLoader());
            } catch (Throwable t) {
                LOGGER.warn("Failed to initialize module entrypoint {}", name, t);
            }
        }
        TheMod.init();
        MinecraftForge.EVENT_BUS.addListener(BiomeSourceWarmup::onLevelLoad);
        ConfigSystem.flushConfig();
        reportCompat();
        com.misanthropy.fastchunkgen.client.ClientSetup.init();
    }

    private static void reportCompat() {
        if (ModCompat.isLithiumFamilyPresent()) {
            LOGGER.info("Lithium-family mod detected, disabled our duplicate NBT copy patch");
        }
        if (ModCompat.isCanaryPresent()) {
            LOGGER.info("Canary detected, deferred our DistanceManager holder iteration patch to its chunk.replace_streams");
        }
        if (ModCompat.isModernFixFeatureEnabled("mixin.perf.worldgen_allocation")) {
            if (com.misanthropy.fastchunkgen.opts.allocs.Config.overrideModernFixWorldGenAllocations) {
                LOGGER.info("ModernFix worldgen_allocation detected, applied our world generation allocation patches ahead of it");
            } else {
                LOGGER.info("ModernFix worldgen_allocation detected, deferred our world generation allocation patches to it");
            }
        }
        if (ModCompat.isModernFixFeatureEnabled("mixin.perf.release_protochunks")
                && com.misanthropy.fastchunkgen.threading.worldgen.ModuleEntryPoint.enabled) {
            LOGGER.warn("ModernFix mixin.perf.release_protochunks is enabled together with threaded world generation.");
            LOGGER.warn("It reads ChunkMap.updatingChunkMap from scheduleChunkGeneration, so async scheduling was disabled..");
            LOGGER.warn("Set mixin.perf.release_protochunks=false in config/modernfix-mixins.properties to get it back.");
        }
    }
}
