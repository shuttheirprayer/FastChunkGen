package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.base.common.util.Log;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface_fold.ISurfaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class SurfaceFold {

    private static final int MAX_DEPTH = 48;
    private static final int CANARY_CALLS = 64;
    private static final int CHECK_EVERY = 4096;
    private static final Dead NONE = new Dead(Long.MIN_VALUE, Long.MAX_VALUE);
    private static final Dead ALL = new Dead(Long.MAX_VALUE, Long.MIN_VALUE);
    private static volatile boolean off;

    private record Dead(long below, long from) {

        Dead union(Dead other) {
            return new Dead(Math.max(this.below, other.below), Math.min(this.from, other.from));
        }

        Dead intersect(Dead other) {
            return new Dead(Math.min(this.below, other.below), Math.max(this.from, other.from));
        }

        boolean trivial() {
            return this.below == Long.MIN_VALUE && this.from == Long.MAX_VALUE;
        }
    }

    private SurfaceFold() {
    }

    public static Plan planFor(List<SurfaceRules.RuleSource> sources, SurfaceRules.Context context, Plan known) {
        if (off || sources.size() < 2) return null;
        try {
            final WorldGenerationContext generation = ((ISurfaceContext) (Object) context).fcg$getGenerationContext();
            if (generation == null) return null;
            final int minY = generation.getMinGenY();
            final int height = generation.getGenDepth();
            if (known != null && known.minY == minY && known.height == height) return known;
            final int size = sources.size();
            final long[] below = new long[size];
            final long[] from = new long[size];
            boolean skippable = false;
            for (int i = 0; i < size; i++) {
                final Dead dead = dead(sources.get(i), generation, 0);
                below[i] = dead.below;
                from[i] = dead.from;
                skippable |= !dead.trivial();
            }
            return new Plan(minY, height, skippable, below, from);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Dead dead(SurfaceRules.RuleSource source, WorldGenerationContext generation, int depth) {
        if (depth > MAX_DEPTH) return NONE;
        final Class<?> type = source.getClass();
        if (type == SurfaceRules.TestRuleSource.class) {
            final SurfaceRules.TestRuleSource test = (SurfaceRules.TestRuleSource) source;
            return deadCondition(test.ifTrue(), generation, depth + 1).union(dead(test.thenRun(), generation, depth + 1));
        }
        if (type == SurfaceRules.SequenceRuleSource.class) {
            Dead dead = ALL;
            for (SurfaceRules.RuleSource element : ((SurfaceRules.SequenceRuleSource) source).sequence()) {
                dead = dead.intersect(dead(element, generation, depth + 1));
            }
            return dead;
        }
        return NONE;
    }

    private static Dead deadCondition(SurfaceRules.ConditionSource condition, WorldGenerationContext generation, int depth) {
        if (depth > MAX_DEPTH) return NONE;
        final Class<?> type = condition.getClass();
        if (type == SurfaceRules.YConditionSource.class) {
            final SurfaceRules.YConditionSource y = (SurfaceRules.YConditionSource) condition;
            if (y.surfaceDepthMultiplier() != 0 || y.addStoneDepth()) return NONE;
            return new Dead(y.anchor().resolveY(generation), Long.MAX_VALUE);
        }
        if (type == SurfaceRules.VerticalGradientConditionSource.class) {
            final SurfaceRules.VerticalGradientConditionSource gradient = (SurfaceRules.VerticalGradientConditionSource) condition;
            final long trueAtAndBelow = gradient.trueAtAndBelow().resolveY(generation);
            final long falseAtAndAbove = gradient.falseAtAndAbove().resolveY(generation);
            return new Dead(Long.MIN_VALUE, Math.max(falseAtAndAbove, trueAtAndBelow + 1L));
        }
        if (type == SurfaceRules.NotConditionSource.class) {
            final SurfaceRules.ConditionSource inner = ((SurfaceRules.NotConditionSource) condition).target();
            final Class<?> innerType = inner.getClass();
            if (innerType == SurfaceRules.YConditionSource.class) {
                final SurfaceRules.YConditionSource y = (SurfaceRules.YConditionSource) inner;
                if (y.surfaceDepthMultiplier() != 0 || y.addStoneDepth()) return NONE;
                return new Dead(Long.MIN_VALUE, y.anchor().resolveY(generation));
            }
            if (innerType == SurfaceRules.VerticalGradientConditionSource.class) {
                final SurfaceRules.VerticalGradientConditionSource gradient = (SurfaceRules.VerticalGradientConditionSource) inner;
                return new Dead(gradient.trueAtAndBelow().resolveY(generation) + 1L, Long.MAX_VALUE);
            }
            if (innerType == SurfaceRules.NotConditionSource.class) {
                return deadCondition(((SurfaceRules.NotConditionSource) inner).target(), generation, depth + 1);
            }
        }
        return NONE;
    }

    private static void switchOff(int blockY, BlockState folded, BlockState full) {
        if (!off) {
            off = true;
            Log.MAIN.error("Surface rule fold switched off for this run: at y={} the folded rules gave {} and the full rules gave {}", blockY, folded, full);
        }
    }

    public static final class Plan {

        private final int minY;
        private final int height;
        private final boolean skippable;
        private final long[] below;
        private final long[] from;
        private final int[] all;
        private final AtomicReferenceArray<int[]> byY;

        private Plan(int minY, int height, boolean skippable, long[] below, long[] from) {
            this.minY = minY;
            this.height = Math.max(height, 0);
            this.skippable = skippable;
            this.below = below;
            this.from = from;
            this.all = new int[below.length];
            for (int i = 0; i < this.all.length; i++) this.all[i] = i;
            this.byY = skippable ? new AtomicReferenceArray<>(this.height) : null;
        }

        public SurfaceRules.SurfaceRule build(List<SurfaceRules.RuleSource> sources, SurfaceRules.Context context) {
            if (!this.skippable || off) return null;
            final SurfaceRules.SurfaceRule[] rules = new SurfaceRules.SurfaceRule[sources.size()];
            for (int i = 0; i < rules.length; i++) rules[i] = sources.get(i).apply(context);
            return new Folded(rules, this, context);
        }

        private int[] keep(int blockY) {
            final int index = blockY - this.minY;
            if (index < 0 || index >= this.height) return this.all;
            int[] keep = this.byY.get(index);
            if (keep == null) {
                keep = this.compute(blockY);
                this.byY.set(index, keep);
            }
            return keep;
        }

        private int[] compute(int blockY) {
            int kept = 0;
            for (int i = 0; i < this.all.length; i++) {
                if (blockY >= this.below[i] && blockY < this.from[i]) kept++;
            }
            if (kept == this.all.length) return this.all;
            final int[] keep = new int[kept];
            kept = 0;
            for (int i = 0; i < this.all.length; i++) {
                if (blockY >= this.below[i] && blockY < this.from[i]) keep[kept++] = i;
            }
            return keep;
        }
    }

    private static final class Folded implements SurfaceRules.SurfaceRule {

        private final SurfaceRules.SurfaceRule[] rules;
        private final Plan plan;
        private final ISurfaceContext context;
        private int calls;

        private Folded(SurfaceRules.SurfaceRule[] rules, Plan plan, SurfaceRules.Context context) {
            this.rules = rules;
            this.plan = plan;
            this.context = (ISurfaceContext) (Object) context;
        }

        @Override
        public BlockState tryApply(int x, int y, int z) {
            if (off) return this.full(x, y, z);
            final int blockY = this.context.fcg$getBlockY();
            final int[] keep = this.plan.keep(blockY);
            final SurfaceRules.SurfaceRule[] rules = this.rules;
            BlockState result = null;
            if (keep.length == rules.length) {
                for (SurfaceRules.SurfaceRule rule : rules) {
                    result = rule.tryApply(x, y, z);
                    if (result != null) break;
                }
            } else {
                for (int index : keep) {
                    result = rules[index].tryApply(x, y, z);
                    if (result != null) break;
                }
            }
            final int n = ++this.calls;
            if (ShortcutVerify.ENABLED || n <= CANARY_CALLS || (n & (CHECK_EVERY - 1)) == 0) {
                final BlockState expected = this.full(x, y, z);
                if (ShortcutVerify.ENABLED) ShortcutVerify.SURFACE_FOLD.record(expected == result, blockY + ": " + result + " vs " + expected);
                if (expected != result) {
                    switchOff(blockY, result, expected);
                    return expected;
                }
            }
            return result;
        }

        private BlockState full(int x, int y, int z) {
            for (SurfaceRules.SurfaceRule rule : this.rules) {
                final BlockState result = rule.tryApply(x, y, z);
                if (result != null) return result;
            }
            return null;
        }
    }
}
