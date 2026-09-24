package com.misanthropy.fastchunkgen.threading.worldgen.common.debug;


import com.google.common.collect.Sets;
import com.misanthropy.fastchunkgen.threading.worldgen.common.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

// Used to examine getChunk calls with reduced lock radius
public class StacktraceRecorder {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int recordFrequency = Mth.clamp(Integer.getInteger("com.misanthropy.fastchunkgen.common.threading.worldgen.debug.DebugReducedLockRadiusFrequency", 4), 1, 16);
    private static final long frequencyBitMask = (1L << recordFrequency) - 1;

    private static final Set<StacktraceHolder> recordedStacktraces = Sets.newConcurrentHashSet();
    private static final AtomicLong sampledCount = new AtomicLong();

    public static void record() {
        if (!Config.debugReducedLockRadius) return;
        if ((sampledCount.incrementAndGet() & frequencyBitMask) != 0) return;
        final StacktraceHolder stacktraceHolder = new StacktraceHolder();
        if (recordedStacktraces.add(stacktraceHolder)) {
            if (stacktraceHolder.needPrint()) {
                LOGGER.warn("Potential dangerous call with reducedLockRadius", stacktraceHolder.throwable);
            } else {
//                LOGGER.info("Ignoring safe call");
            }
        }
    }


    public static class StacktraceHolder {

        private static final String StructureProcessor$process = "m_7382_"; // processBlock
        private static final String BlendingData$getBlendingData = "m_190304_"; // getOrUpdateBlendingData
        private static final String ChunkGenerator$carve = "m_213679_"; // applyCarvers
        private static final String SpawnHelper$populateEntities = "m_220450_"; // spawnMobsForChunkGeneration
        private static final String StructureAccessor$method_41032 = "m_220480_"; // fillStartsForStructure
        private static final String BiomeAccess$Storage$getBiomeForNoiseGen = "m_203495_"; // getNoiseBiome
        private static final String BlockCollisionSpliterator$getChunk = "m_186411_"; // getChunk

        @NotNull
        private final StackTraceElement[] stackTrace;
        private final Throwable throwable;

        public StacktraceHolder() {
            this.throwable = new Throwable();
            this.stackTrace = this.throwable.getStackTrace();
        }

        public boolean needPrint() {
            for (StackTraceElement stackTraceElement : stackTrace) {
                if (stackTraceElement.getMethodName().equals("method_26971")) // TODO: what is this?
                    return false;
                if (stackTraceElement.getClassName().equals(RuleProcessor.class.getName()) &&
                        stackTraceElement.getMethodName().equals(StructureProcessor$process))
                    return false;
                if (stackTraceElement.getClassName().equals(BlendingData.class.getName()) &&
                        stackTraceElement.getMethodName().equals(BlendingData$getBlendingData))
                    return false;
                if (stackTraceElement.getClassName().equals(NoiseBasedChunkGenerator.class.getName()) &&
                        stackTraceElement.getMethodName().equals(ChunkGenerator$carve))
                    return false;
                if (stackTraceElement.getClassName().equals(NaturalSpawner.class.getName()) &&
                        stackTraceElement.getMethodName().equals(SpawnHelper$populateEntities))
                    return false;
                if (stackTraceElement.getClassName().equals(StructureManager.class.getName()) &&
                        stackTraceElement.getMethodName().equals(StructureAccessor$method_41032))
                    return false;
                if (stackTraceElement.getClassName().equals(LevelReader.class.getName()) &&
                        stackTraceElement.getMethodName().equals(BiomeAccess$Storage$getBiomeForNoiseGen))
                    return false;
                if (stackTraceElement.getClassName().equals(BlockCollisions.class.getName()) &&
                        stackTraceElement.getMethodName().equals(BlockCollisionSpliterator$getChunk))
                    return false;

                // lithium
                if (stackTraceElement.getClassName().equals("me.jellysquid.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper"))
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "StacktraceHolder{" +
                    "stackTrace=" + Arrays.toString(stackTrace) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StacktraceHolder that = (StacktraceHolder) o;
            return Arrays.equals(stackTrace, that.stackTrace);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(stackTrace);
        }
    }

}
