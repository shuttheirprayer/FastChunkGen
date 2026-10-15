package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.INoiseInterpolator;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class SharedNoise {

    public static final int NO_LEVEL = Integer.MIN_VALUE;
    private static final int MAX_COLUMNS = 1 << 13, MAX_LEVELS = 1 << 18;

    private static final Map<RandomState, SharedNoise> BY_STATE = Collections.synchronizedMap(new WeakHashMap<>());

    private static final String DENSITY_FUNCTIONS = "net.minecraft.world.level.levelgen.DensityFunctions$";
    private static final String NOISE_CHUNK = "net.minecraft.world.level.levelgen.NoiseChunk$";
    private static final int IMPURE = 0, PURE = 1, FREE = 2, CONST = 3;
    private static final int SLICE = 0, POINT = 1, OUTSIDE = 2;
    private static final byte UNKNOWN = 0, NO = 1, YES = 2;

    private final int minY, height, cellWidth, cellHeight;
    private volatile byte columnsShareable, levelsShareable;
    private final Long2ObjectOpenHashMap<Column> columns = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap levels = new Long2IntOpenHashMap();

    private static final class Column {
        final double[][] values;
        final long counterSteps;
        int remaining;

        Column(double[][] values, long counterSteps, int remaining) {
            this.values = values;
            this.counterSteps = counterSteps;
            this.remaining = remaining;
        }
    }

    private SharedNoise(int minY, int height, int cellWidth, int cellHeight) {
        this.minY = minY;
        this.height = height;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.levels.defaultReturnValue(NO_LEVEL);
    }

    public static SharedNoise of(RandomState state, int minY, int height, int cellWidth, int cellHeight) {
        final SharedNoise shared = BY_STATE.computeIfAbsent(state, unused -> new SharedNoise(minY, height, cellWidth, cellHeight));
        return shared.minY == minY && shared.height == height && shared.cellWidth == cellWidth && shared.cellHeight == cellHeight ? shared : null;
    }

    public boolean columnsShareable(List<NoiseChunk.NoiseInterpolator> interpolators, NoiseRouter router, List<?> cellCaches) {
        if (this.columnsShareable == UNKNOWN) {
            this.columnsShareable = sliceIsolated(interpolators, router, cellCaches) ? YES : NO;
        }
        return this.columnsShareable == YES;
    }

    public boolean levelsShareable(DensityFunction initialDensity) {
        if (this.levelsShareable == UNKNOWN) {
            this.levelsShareable = kind(initialDensity, POINT, null, new IdentityHashMap<>(), 0) != IMPURE ? YES : NO;
        }
        return this.levelsShareable == YES;
    }

    public long takeColumn(long key, List<NoiseChunk.NoiseInterpolator> interpolators, boolean slice0, int i) {
        final Column column;
        synchronized (this.columns) {
            column = this.columns.get(key);
            if (column == null) return -1L;
            if (--column.remaining <= 0) this.columns.remove(key);
        }
        if (column.values.length != interpolators.size()) return -1L;
        for (int k = 0; k < column.values.length; k++) {
            if (slot(interpolators.get(k), slice0, i).length != column.values[k].length) return -1L;
        }
        for (int k = 0; k < column.values.length; k++) {
            final double[] slot = slot(interpolators.get(k), slice0, i);
            System.arraycopy(column.values[k], 0, slot, 0, slot.length);
        }
        return column.counterSteps;
    }

    public void putColumn(long key, int sharers, List<NoiseChunk.NoiseInterpolator> interpolators, boolean slice0, int i, long counterSteps) {
        final double[][] values = new double[interpolators.size()][];
        for (int k = 0; k < values.length; k++) values[k] = slot(interpolators.get(k), slice0, i).clone();
        synchronized (this.columns) {
            final Column raced = this.columns.get(key);
            if (raced != null) {
                if (--raced.remaining <= 0) this.columns.remove(key);
                return;
            }
            if (this.columns.size() >= MAX_COLUMNS) this.columns.clear();
            this.columns.put(key, new Column(values, counterSteps, sharers - 1));
        }
    }

    public int level(long column) {
        synchronized (this.levels) {
            return this.levels.get(column);
        }
    }

    public void putLevel(long column, int level) {
        synchronized (this.levels) {
            if (this.levels.size() >= MAX_LEVELS) this.levels.clear();
            this.levels.put(column, level);
        }
    }

    private static double[] slot(NoiseChunk.NoiseInterpolator interpolator, boolean slice0, int i) {
        final INoiseInterpolator corners = (INoiseInterpolator) interpolator;
        return (slice0 ? corners.fcg$slice0() : corners.fcg$slice1())[i];
    }

    private static boolean sliceIsolated(List<NoiseChunk.NoiseInterpolator> interpolators, NoiseRouter router, List<?> cellCaches) {
        final Set<Object> inside = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Object, Integer> memo = new IdentityHashMap<>();
        for (NoiseChunk.NoiseInterpolator interpolator : interpolators) {
            if (kind(interpolator.wrapped(), SLICE, inside, memo, 0) == IMPURE) return false;
        }
        final Set<Object> outside = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Object, Integer> outsideMemo = new IdentityHashMap<>();
        final Field[] routerParts = CellBounds.COMPONENTS.get(NoiseRouter.class);
        if (routerParts.length == 0) return false;
        try {
            for (Field part : routerParts) {
                if (kind(part.get(router), OUTSIDE, outside, outsideMemo, 0) == IMPURE) return false;
            }
        } catch (ReflectiveOperationException e) {
            return false;
        }
        for (Object cache : cellCaches) {
            if (kind(cache, OUTSIDE, outside, outsideMemo, 0) == IMPURE) return false;
        }
        for (Object cache : inside) if (outside.contains(cache)) return false;
        return true;
    }

    private static int kind(Object node, int mode, Set<Object> cacheOnces, Map<Object, Integer> memo, int depth) {
        if (depth > 128) return IMPURE;
        final Integer known = memo.get(node);
        if (known != null) return known;
        int kind;
        try {
            kind = kind0(node, mode, cacheOnces, memo, depth);
        } catch (ReflectiveOperationException | RuntimeException e) {
            kind = IMPURE;
        }
        memo.put(node, kind);
        return kind;
    }

    @SuppressWarnings("unchecked")
    private static int kind0(Object node, int mode, Set<Object> cacheOnces, Map<Object, Integer> memo, int depth) throws ReflectiveOperationException {
        final String name = node.getClass().getName();
        if (node instanceof DensityFunctions.MarkerOrMarked marked && name.startsWith(NOISE_CHUNK)) {
            final String type = name.substring(NOISE_CHUNK.length());
            if (mode == OUTSIDE && type.equals("NoiseInterpolator")) return FREE;
            final int inner = kind(marked.wrapped(), mode, cacheOnces, memo, depth + 1);
            if (mode != POINT && type.equals("CacheOnce") && inner != CONST) cacheOnces.add(node);
            return switch (type) {
                case "NoiseInterpolator", "CacheAllInCell" -> mode == SLICE ? IMPURE : inner;
                case "CacheOnce" -> inner;
                case "FlatCache", "Cache2D" -> mode == OUTSIDE || inner >= FREE ? inner : IMPURE;
                default -> IMPURE;
            };
        }
        if (node instanceof BlendedNoise) return PURE;
        // a beardifier predates this chunk's wrappers and differs per chunk, while the verdict is cached per RandomState
        if (mode == OUTSIDE && (node instanceof DensityFunctions.BeardifierOrMarker || name.startsWith(NOISE_CHUNK))) return PURE;
        if (!name.startsWith(DENSITY_FUNCTIONS)) return IMPURE;
        final Field[] parts = CellBounds.COMPONENTS.get(node.getClass());
        return switch (name.substring(DENSITY_FUNCTIONS.length())) {
            case "Constant", "BlendAlpha", "BlendOffset" -> CONST;
            case "ShiftA", "ShiftB", "EndIslandDensityFunction" -> FREE;
            case "YClampedGradient", "Shift" -> PURE;
            case "Noise" -> (double) parts[2].get(node) == 0.0D ? FREE : PURE;
            case "ShiftedNoise" -> Math.min(Math.min(child(node, parts[0], mode, cacheOnces, memo, depth), child(node, parts[1], mode, cacheOnces, memo, depth)),
                    Math.min(child(node, parts[2], mode, cacheOnces, memo, depth), (double) parts[4].get(node) == 0.0D ? FREE : PURE));
            case "WeirdScaledSampler" -> Math.min(child(node, parts[0], mode, cacheOnces, memo, depth), PURE);
            case "Ap2" -> Math.min(child(node, parts[1], mode, cacheOnces, memo, depth), child(node, parts[2], mode, cacheOnces, memo, depth));
            case "MulOrAdd", "Mapped" -> child(node, parts[1], mode, cacheOnces, memo, depth);
            case "Clamp", "BlendDensity" -> child(node, parts[0], mode, cacheOnces, memo, depth);
            case "RangeChoice" -> Math.min(child(node, parts[0], mode, cacheOnces, memo, depth),
                    Math.min(child(node, parts[3], mode, cacheOnces, memo, depth), child(node, parts[4], mode, cacheOnces, memo, depth)));
            case "HolderHolder" -> kind(((Holder<DensityFunction>) parts[0].get(node)).value(), mode, cacheOnces, memo, depth + 1);
            case "Spline" -> spline(((DensityFunctions.Spline) node).spline(), mode, cacheOnces, memo, depth + 1);
            default -> IMPURE;
        };
    }

    private static int child(Object node, Field part, int mode, Set<Object> cacheOnces, Map<Object, Integer> memo, int depth) throws ReflectiveOperationException {
        return kind(part.get(node), mode, cacheOnces, memo, depth + 1);
    }

    private static int spline(CubicSpline<?, ?> spline, int mode, Set<Object> cacheOnces, Map<Object, Integer> memo, int depth) {
        if (depth > 128) return IMPURE;
        if (spline instanceof CubicSpline.Constant<?, ?>) return CONST;
        if (!(spline instanceof CubicSpline.Multipoint<?, ?> multipoint)
                || !(multipoint.coordinate() instanceof DensityFunctions.Spline.Coordinate coordinate)) return IMPURE;
        int kind = kind(coordinate.function().value(), mode, cacheOnces, memo, depth + 1);
        for (CubicSpline<?, ?> value : multipoint.values()) kind = Math.min(kind, spline(value, mode, cacheOnces, memo, depth + 1));
        return kind;
    }

}
