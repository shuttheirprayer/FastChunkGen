package com.misanthropy.fastchunkgen.client.embeddium;

import java.util.HashMap;
import java.util.Map;

class OptionText {

    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, String> TOOLTIPS = new HashMap<>();

    static {
        NAMES.put("globalExecutorParallelism", "Worker Threads");
        TOOLTIPS.put("globalExecutorParallelism", "Worker threads used for chunk generation and IO.");
        NAMES.put("threadedWorldGen.enabled", "Threaded World Generation");
        TOOLTIPS.put("threadedWorldGen.enabled", "Generate chunks on multiple threads.");
        NAMES.put("threadedWorldGen.allowThreadedFeatures", "Threaded Features");
        TOOLTIPS.put("threadedWorldGen.allowThreadedFeatures", "Generate features like trees, ores and structures in parallel.");
        NAMES.put("threadedWorldGen.reduceLockRadius", "Reduced Lock Radius");
        TOOLTIPS.put("threadedWorldGen.reduceLockRadius", "Lock fewer neighbouring chunks per generation step for more parallelism.");
        NAMES.put("threadedWorldGen.asyncScheduling", "Async Scheduling");
        TOOLTIPS.put("threadedWorldGen.asyncScheduling", "Schedule generation tasks off the server thread.");
        NAMES.put("maxViewDistance", "Max Render Distance");
        TOOLTIPS.put("maxViewDistance", "Raises the 32 chunk render distance cap. Needs a restart and much more memory at high values.");
        NAMES.put("threadedLighting.enabled", "Threaded Lighting");
        TOOLTIPS.put("threadedLighting.enabled", "Run light updates on a dedicated thread.");
        NAMES.put("ioSystem.async", "Async Chunk IO");
        TOOLTIPS.put("ioSystem.async", "Load and unload chunks off the server thread.");
        NAMES.put("ioSystem.replaceImpl", "Rewritten IO");
        TOOLTIPS.put("ioSystem.replaceImpl", "Use the rewritten chunk IO implementation.");
        NAMES.put("ioSystem.optimizations", "Chunk IO Optimizations");
        TOOLTIPS.put("ioSystem.optimizations", "Enable chunk IO optimizations.");
        NAMES.put("ioSystem.serializeBlockEntitiesOnMainThread", "Safe Block Entity Saving");
        TOOLTIPS.put("ioSystem.serializeBlockEntitiesOnMainThread", "Snapshot block entity data on the server thread. Turning this off is faster but can corrupt modded block entities.");
        NAMES.put("ioSystem.fireForgeChunkDataEvents", "Fire Chunk Data Events");
        TOOLTIPS.put("ioSystem.fireForgeChunkDataEvents", "Fire Forge ChunkDataEvent.Save on async saves. Off means mods lose per-chunk data.");
        NAMES.put("ioSystem.forgeChunkDataEventsOnMainThread", "Chunk Data Events On Server Thread");
        TOOLTIPS.put("ioSystem.forgeChunkDataEventsOnMainThread", "Fire Forge chunk data events on the server thread. Off is unsafe for most mods.");
        NAMES.put("ioSystem.chunkDataCacheSoftLimit", "Chunk Cache Soft Limit");
        TOOLTIPS.put("ioSystem.chunkDataCacheSoftLimit", "Cached chunk writes before flushing to disk starts.");
        NAMES.put("ioSystem.chunkDataCacheLimit", "Chunk Cache Hard Limit");
        TOOLTIPS.put("ioSystem.chunkDataCacheLimit", "Hard cap on cached chunk writes.");
        NAMES.put("ioSystem.chunkStreamVersion", "Chunk Compression");
        TOOLTIPS.put("ioSystem.chunkStreamVersion", "Compression for newly saved chunks. -1 vanilla, 1 GZip, 2 Zlib, 3 none.");
        NAMES.put("generalOptimizations.optimizeScheduling", "Optimize Scheduling");
        TOOLTIPS.put("generalOptimizations.optimizeScheduling", "Optimize chunk task scheduling.");
        NAMES.put("generalOptimizations.optimizeAsyncChunkRequest", "Async Chunk Requests");
        TOOLTIPS.put("generalOptimizations.optimizeAsyncChunkRequest", "Serve off-thread chunk requests without blocking the server thread.");
        NAMES.put("generalOptimizations.reduceAllocations", "Reduce Allocations");
        TOOLTIPS.put("generalOptimizations.reduceAllocations", "Reduce allocations in hot paths.");
        NAMES.put("generalOptimizations.overrideModernFixWorldGenAllocations", "Override ModernFix Allocations");
        TOOLTIPS.put("generalOptimizations.overrideModernFixWorldGenAllocations", "Apply our world generation allocation patches ahead of ModernFix's equivalent ones.");
        NAMES.put("generalOptimizations.midTickChunkTasksInterval", "Mid Tick Chunk Tasks");
        TOOLTIPS.put("generalOptimizations.midTickChunkTasksInterval", "Nanoseconds between chunk tasks run inside the tick loop.");
        NAMES.put("generalOptimizations.autoSave.mode", "Auto Save Mode");
        TOOLTIPS.put("generalOptimizations.autoSave.mode", "Vanilla saves every tick, Enhanced saves during idle time, Periodic saves every 6000 ticks.");
        NAMES.put("generalOptimizations.autoSave.delay", "Auto Save Delay");
        TOOLTIPS.put("generalOptimizations.autoSave.delay", "Milliseconds before a dirty chunk is auto-saved. Enhanced mode only.");
        NAMES.put("vanillaWorldGenOptimizations.enabled", "Vanilla World Gen Optimizations");
        TOOLTIPS.put("vanillaWorldGenOptimizations.enabled", "Optimize vanilla world generation.");
        NAMES.put("vanillaWorldGenOptimizations.optimizeAquifer", "Faster Aquifers");
        TOOLTIPS.put("vanillaWorldGenOptimizations.optimizeAquifer", "Faster aquifer sampling.");
        NAMES.put("vanillaWorldGenOptimizations.useEndBiomeCache", "End Biome Cache");
        TOOLTIPS.put("vanillaWorldGenOptimizations.useEndBiomeCache", "Cache End biome lookups.");
        NAMES.put("vanillaWorldGenOptimizations.optimizeRandomInstances", "Cheaper Random Instances");
        TOOLTIPS.put("vanillaWorldGenOptimizations.optimizeRandomInstances", "Cheaper random instances during world generation.");
        NAMES.put("fixes.generalThreadingIssues", "Chunk System Thread Fixes");
        TOOLTIPS.put("fixes.generalThreadingIssues", "Fix thread-safety issues in the vanilla chunk system.");
        NAMES.put("fixes.chunkIoThreadingIssues", "Chunk IO Thread Fixes");
        TOOLTIPS.put("fixes.chunkIoThreadingIssues", "Fix thread-safety issues in vanilla chunk IO.");
        NAMES.put("fixes.worldGenThreadingIssues", "World Gen Thread Fixes");
        TOOLTIPS.put("fixes.worldGenThreadingIssues", "Fix thread-safety issues in vanilla world generation. Required by threaded world generation.");
        NAMES.put("fixes.vanillaWorldGenBugs", "Vanilla World Gen Bug Fixes");
        TOOLTIPS.put("fixes.vanillaWorldGenBugs", "Fix vanilla world generation bugs.");
        NAMES.put("fixes.disableLoggingShutdownHook", "Faster Shutdown");
        TOOLTIPS.put("fixes.disableLoggingShutdownHook", "Remove the log4j2 shutdown hook and force exit if a dedicated server hangs.");
        NAMES.put("fixes.enforceSafeWorldRandomAccess", "Strict Random Access");
        TOOLTIPS.put("fixes.enforceSafeWorldRandomAccess", "Crash instead of warn when something uses the world random off-thread. For debugging.");
        NAMES.put("ioSystem.gcFreeChunkSerializer", "GC Free Chunk Serializer");
        TOOLTIPS.put("ioSystem.gcFreeChunkSerializer", "Allocation free chunk writer. Cannot fire Forge chunk data events, so mods lose per-chunk data.");
        NAMES.put("ioSystem.recoverFromErrors", "Recover From Errors");
        TOOLTIPS.put("ioSystem.recoverFromErrors", "Regenerate chunks that fail to load. Destroys their contents.");
        NAMES.put("generalOptimizations.autoSave.mode.VANILLA", "Vanilla");
        NAMES.put("generalOptimizations.autoSave.mode.ENHANCED", "Enhanced");
        NAMES.put("generalOptimizations.autoSave.mode.PERIODIC", "Periodic");
    }

    static String name(String key) {
        return NAMES.getOrDefault(key, key);
    }

    static String tooltip(String key) {
        return TOOLTIPS.getOrDefault(key, "");
    }
}
