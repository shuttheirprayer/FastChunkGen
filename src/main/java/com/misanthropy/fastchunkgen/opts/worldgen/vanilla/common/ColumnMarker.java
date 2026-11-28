package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.core.Holder;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ColumnMarker implements DensityFunction {

    private static final String DENSITY_FUNCTIONS = "net.minecraft.world.level.levelgen.DensityFunctions$";
    private static final String LITHOFORM = "com.misanthropy.lithoform.bowl.";
    private static final int WORTH_CACHING = 12, WORTH_SHARING = 24;
    private static final Map<RandomState, Hoisted> HOISTED = Collections.synchronizedMap(new WeakHashMap<>());

    private record Hoisted(NoiseRouter from, NoiseRouter to) {
    }

    final DensityFunction inner;
    private final int id;
    private final boolean point;

    private ColumnMarker(DensityFunction inner, int id, boolean point) {
        this.inner = inner;
        this.id = id;
        this.point = point;
    }

    public DensityFunction wrap(int minX, int minZ, int size) {
        if (ShortcutVerify.COLUMN_NOISE.broken()) return this.inner;
        return this.point ? new PointNoise(this.inner) : new ColumnNoise(this.inner, minX, minZ, size);
    }

    public static NoiseRouter hoist(RandomState random, NoiseRouter router) {
        Hoisted hoisted = HOISTED.get(random);
        if (hoisted == null || hoisted.from != router) {
            hoisted = new Hoisted(router, hoisted(router));
            HOISTED.put(random, hoisted);
        }
        return hoisted.to;
    }

    private static NoiseRouter hoisted(NoiseRouter r) {
        DensityFunction density;
        try {
            final Hoister hoister = new Hoister();
            final DensityFunction columns = r.finalDensity().mapAll(hoister);
            density = columns.mapAll(hoister.sharer(columns));
        } catch (RuntimeException e) {
            density = r.finalDensity();
        }
        final Map<DensityFunction, DensityFunction> interned = new HashMap<>();
        return new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(), r.fluidLevelSpreadNoise(), r.lavaNoise(),
                r.temperature(), r.vegetation(), r.continents(), r.erosion(), r.depth(), r.ridges(),
                r.initialDensityWithoutJaggedness(), density, r.veinToggle(), r.veinRidged(), r.veinGap()).mapAll(f -> interned.computeIfAbsent(f, same -> same));
    }

    private static final class Hoister implements Visitor {
        private final Map<DensityFunction, ColumnMarker> markers = new HashMap<>();
        private final IdentityHashMap<Object, Boolean> free = new IdentityHashMap<>();
        private final IdentityHashMap<Object, Integer> costs = new IdentityHashMap<>();
        private final IdentityHashMap<Object, Boolean> pure = new IdentityHashMap<>();

        @Override
        public DensityFunction apply(DensityFunction function) {
            if ("FlatCache".equals(markerType(function))) {
                return strip(function);
            }
            if (!this.free(function)) return function;
            final DensityFunction plain = strip(function);
            if (this.cost(plain) < WORTH_CACHING) return plain;
            ColumnMarker column = this.markers.get(plain);
            if (column == null) {
                column = new ColumnMarker(plain, this.markers.size(), false);
                this.markers.put(plain, column);
            }
            return column;
        }

        private static DensityFunction strip(DensityFunction function) {
            return function.mapAll(f -> f instanceof ColumnMarker marker ? marker.inner : f);
        }

        private Visitor sharer(DensityFunction root) {
            final Map<DensityFunction, Integer> uses = new HashMap<>();
            this.count(root, uses);
            final Map<DensityFunction, ColumnMarker> shared = new HashMap<>();
            for (Map.Entry<DensityFunction, Integer> use : uses.entrySet()) {
                final DensityFunction function = use.getKey();
                if (use.getValue() > 1 && !(function instanceof ColumnMarker) && this.cost(function) >= WORTH_SHARING && this.pure(function)) {
                    shared.put(function, new ColumnMarker(function, this.markers.size() + shared.size(), true));
                }
            }
            return function -> {
                if (function instanceof ColumnMarker column) {
                    return new ColumnMarker(strip(column.inner), column.id, column.point);
                }
                if ("FlatCache".equals(markerType(function))) return strip(function);
                if (shared.isEmpty()) return function;
                final DensityFunction plain = function.mapAll(f -> f instanceof ColumnMarker marker && marker.point ? marker.inner : f);
                final ColumnMarker marker = shared.get(plain);
                return marker == null ? function : new ColumnMarker(function, marker.id, true);
            };
        }

        private void count(Object node, Map<DensityFunction, Integer> uses) throws IllegalStateException {
            if (node instanceof Holder<?> holder) {
                this.count(holder.value(), uses);
                return;
            }
            if (node instanceof CubicSpline<?, ?>) {
                if (node instanceof CubicSpline.Multipoint<?, ?> multipoint) {
                    if (multipoint.coordinate() instanceof DensityFunctions.Spline.Coordinate coordinate) this.count(coordinate.function(), uses);
                    for (CubicSpline<?, ?> value : multipoint.values()) this.count(value, uses);
                }
                return;
            }
            if (!(node instanceof DensityFunction function)) return;
            if (uses.merge(function, 1, Integer::sum) > 1 || node instanceof ColumnMarker) return;
            final String marker = markerType(node);
            if (marker != null) {
                if (!marker.equals("FlatCache")) this.count(((DensityFunctions.MarkerOrMarked) node).wrapped(), uses);
                return;
            }
            try {
                for (Field part : CellBounds.COMPONENTS.get(node.getClass())) this.count(part.get(node), uses);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        private boolean pure(Object node) {
            final Boolean known = this.pure.get(node);
            if (known != null) return known;
            boolean result;
            try {
                result = this.pure0(node);
            } catch (ReflectiveOperationException e) {
                result = false;
            }
            this.pure.put(node, result);
            return result;
        }

        private boolean pure0(Object node) throws ReflectiveOperationException {
            if (node instanceof ColumnMarker || node instanceof BlendedNoise) return true;
            if (node instanceof Holder<?> holder) return this.pure(holder.value());
            if (node instanceof CubicSpline.Constant<?, ?>) return true;
            if (node instanceof CubicSpline.Multipoint<?, ?> multipoint) {
                if (!(multipoint.coordinate() instanceof DensityFunctions.Spline.Coordinate coordinate) || !this.pure(coordinate.function())) return false;
                for (CubicSpline<?, ?> value : multipoint.values()) {
                    if (!this.pure(value)) return false;
                }
                return true;
            }
            final String marker = markerType(node);
            if (marker != null) {
                return (marker.equals("FlatCache") || marker.equals("Cache2D")) && this.pure(((DensityFunctions.MarkerOrMarked) node).wrapped());
            }
            final String name = node.getClass().getName();
            final boolean known;
            if (name.startsWith(LITHOFORM)) {
                known = switch (name.substring(LITHOFORM.length())) {
                    case "MouthGate", "BowlOffset", "BowlMask" -> true;
                    default -> false;
                };
            } else {
                known = name.startsWith(DENSITY_FUNCTIONS) && switch (name.substring(DENSITY_FUNCTIONS.length())) {
                    case "Constant", "BlendAlpha", "BlendOffset", "ShiftA", "ShiftB", "Shift", "EndIslandDensityFunction", "Noise", "ShiftedNoise",
                         "YClampedGradient", "WeirdScaledSampler", "Ap2", "MulOrAdd", "Mapped", "Clamp", "RangeChoice", "HolderHolder", "Spline" -> true;
                    default -> false;
                };
            }
            if (!known) return false;
            for (Field part : CellBounds.COMPONENTS.get(node.getClass())) {
                final Object value = part.get(node);
                if (isPart(value) && !this.pure(value)) return false;
            }
            return true;
        }

        private boolean free(Object node) {
            final Boolean known = this.free.get(node);
            if (known != null) return known;
            boolean result;
            try {
                result = this.free0(node);
            } catch (ReflectiveOperationException e) {
                result = false;
            }
            this.free.put(node, result);
            return result;
        }

        private boolean free0(Object node) throws ReflectiveOperationException {
            if (node instanceof ColumnMarker) return true;
            if (node instanceof Holder<?> holder) return this.free(holder.value());
            if (node instanceof CubicSpline<?, ?> spline) return this.freeSpline(spline);
            final String marker = markerType(node);
            if (marker != null) {
                return switch (marker) {
                    case "FlatCache", "Cache2D", "CacheOnce" -> this.free(((DensityFunctions.MarkerOrMarked) node).wrapped());
                    default -> false;
                };
            }
            final String name = node.getClass().getName();
            final Field[] parts = CellBounds.COMPONENTS.get(node.getClass());
            if (name.startsWith(LITHOFORM)) {
                return switch (name.substring(LITHOFORM.length())) {
                    case "MouthGate" -> true;
                    case "BowlOffset", "BowlMask" -> this.freeParts(node, parts);
                    default -> false;
                };
            }
            if (!name.startsWith(DENSITY_FUNCTIONS)) return false;
            return switch (name.substring(DENSITY_FUNCTIONS.length())) {
                case "Constant", "BlendAlpha", "BlendOffset", "ShiftA", "ShiftB", "EndIslandDensityFunction" -> true;
                case "Noise" -> (double) parts[2].get(node) == 0.0D;
                case "ShiftedNoise" -> (double) parts[4].get(node) == 0.0D && this.freeParts(node, parts);
                case "Ap2", "MulOrAdd", "Mapped", "Clamp", "RangeChoice", "HolderHolder", "Spline" -> this.freeParts(node, parts);
                default -> false;
            };
        }

        private boolean freeParts(Object node, Field[] parts) throws ReflectiveOperationException {
            if (parts.length == 0) return false;
            for (Field part : parts) {
                final Object value = part.get(node);
                if (isPart(value) && !this.free(value)) return false;
            }
            return true;
        }

        private boolean freeSpline(CubicSpline<?, ?> spline) {
            if (spline instanceof CubicSpline.Constant<?, ?>) return true;
            if (!(spline instanceof CubicSpline.Multipoint<?, ?> multipoint)
                    || !(multipoint.coordinate() instanceof DensityFunctions.Spline.Coordinate coordinate)
                    || !this.free(coordinate.function())) return false;
            for (CubicSpline<?, ?> value : multipoint.values()) {
                if (!this.free(value)) return false;
            }
            return true;
        }

        private int cost(Object node) {
            final Integer known = this.costs.get(node);
            if (known != null) return known;
            int cost;
            try {
                cost = this.cost0(node);
            } catch (ReflectiveOperationException e) {
                cost = 0;
            }
            cost = Math.min(cost, 1 << 20);
            this.costs.put(node, cost);
            return cost;
        }

        private int cost0(Object node) throws ReflectiveOperationException {
            if (node instanceof Holder<?> holder) return this.cost(holder.value());
            if (node instanceof CubicSpline.Constant<?, ?>) return 0;
            if (node instanceof CubicSpline.Multipoint<?, ?> multipoint) {
                int cost = 4;
                if (multipoint.coordinate() instanceof DensityFunctions.Spline.Coordinate coordinate) cost += this.cost(coordinate.function());
                for (CubicSpline<?, ?> value : multipoint.values()) cost = Math.max(cost, 4 + this.cost(value));
                return cost;
            }
            final String marker = markerType(node);
            if (marker != null) {
                return marker.equals("FlatCache") ? 1 : 1 + this.cost(((DensityFunctions.MarkerOrMarked) node).wrapped());
            }
            if (node instanceof ColumnMarker) return 1;
            final String name = node.getClass().getName();
            if (name.startsWith(LITHOFORM)) return WORTH_CACHING;
            int cost = switch (name.substring(name.indexOf('$') + 1)) {
                case "net.minecraft.world.level.levelgen.synth.BlendedNoise" -> 64;
                case "Noise", "ShiftedNoise", "ShiftA", "ShiftB", "EndIslandDensityFunction" -> 16;
                case "Constant", "BlendAlpha", "BlendOffset" -> 0;
                default -> 1;
            };
            for (Field part : CellBounds.COMPONENTS.get(node.getClass())) {
                final Object value = part.get(node);
                if (isPart(value)) cost += this.cost(value);
            }
            return cost;
        }

        private static String markerType(Object node) {
            return node instanceof DensityFunctions.MarkerOrMarked marker ? ((Enum<?>) (Object) marker.type()).name() : null;
        }

        private static boolean isPart(Object value) {
            return value instanceof DensityFunction || value instanceof CubicSpline<?, ?>
                    || value instanceof Holder<?> holder && holder.value() instanceof DensityFunction;
        }
    }

    @Override
    public double compute(FunctionContext context) {
        return this.inner.compute(context);
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        this.inner.fillArray(array, provider);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ColumnMarker(this.inner.mapAll(visitor), this.id, this.point));
    }

    @Override
    public double minValue() {
        return this.inner.minValue();
    }

    @Override
    public double maxValue() {
        return this.inner.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return this.inner.codec();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ColumnMarker marker && marker.id == this.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}
