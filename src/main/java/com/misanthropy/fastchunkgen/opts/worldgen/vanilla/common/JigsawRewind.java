package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.base.common.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public final class JigsawRewind {

    private static final long CHECK_FIRST = 300L;
    private static final long CHECK_EVERY = 1024L;
    private static final int[] UNUSABLE = new int[0];
    private static final AtomicLong CHECKS = new AtomicLong();
    private static volatile boolean off;

    private JigsawRewind() {
    }

    public static boolean active() {
        return !off;
    }

    public static boolean supported(Object element) {
        final Class<?> type = element.getClass();
        return type == SinglePoolElement.class || type == LegacySinglePoolElement.class;
    }

    public static boolean unusable(int[] bounds) {
        return bounds == UNUSABLE;
    }

    public static int[] record(StructurePoolElement element, StructureTemplateManager manager) {
        final BoundLog log = new BoundLog();
        element.getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, log);
        return log.bad ? UNUSABLE : Arrays.copyOf(log.bounds, log.size);
    }

    public static void replay(RandomSource random, int[] bounds) {
        random.nextInt(4);
        random.nextInt(3);
        random.nextInt(2);
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int bound : bounds) random.nextInt(bound);
        }
    }

    public static boolean shouldCompare(boolean first) {
        if (first) return true;
        final long n = CHECKS.incrementAndGet();
        return n <= CHECK_FIRST || (n & (CHECK_EVERY - 1L)) == 0L;
    }

    public static boolean sameConsumption(StructurePoolElement element, StructureTemplateManager manager, int[] bounds) {
        final long seed = CHECKS.incrementAndGet() * -7046029254386353131L ^ System.identityHashCode(element);
        final RandomSource real = new LegacyRandomSource(seed);
        final RandomSource replayed = new LegacyRandomSource(seed);
        for (Rotation rotation : Rotation.getShuffled(real)) {
            element.getShuffledJigsawBlocks(manager, BlockPos.ZERO, rotation, real);
        }
        replay(replayed, bounds);
        return real.nextLong() == replayed.nextLong() && real.nextInt(1000003) == replayed.nextInt(1000003);
    }

    public static void switchOff(String reason) {
        if (!off) {
            off = true;
            Log.MAIN.error("Jigsaw repeat rewind switched off for this run: {}", reason);
        }
    }

    private static final class BoundLog implements RandomSource {

        private int[] bounds = new int[16];
        private int size;
        private boolean bad;

        @Override
        public int nextInt(int bound) {
            if (bound <= 0) this.bad = true;
            if (this.size == this.bounds.length) this.bounds = Arrays.copyOf(this.bounds, this.size * 2);
            this.bounds[this.size++] = bound;
            return 0;
        }

        @Override
        public RandomSource fork() {
            this.bad = true;
            return this;
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            this.bad = true;
            return null;
        }

        @Override
        public void setSeed(long seed) {
            this.bad = true;
        }

        @Override
        public int nextInt() {
            this.bad = true;
            return 0;
        }

        @Override
        public long nextLong() {
            this.bad = true;
            return 0L;
        }

        @Override
        public boolean nextBoolean() {
            this.bad = true;
            return false;
        }

        @Override
        public float nextFloat() {
            this.bad = true;
            return 0.0F;
        }

        @Override
        public double nextDouble() {
            this.bad = true;
            return 0.0D;
        }

        @Override
        public double nextGaussian() {
            this.bad = true;
            return 0.0D;
        }
    }
}
