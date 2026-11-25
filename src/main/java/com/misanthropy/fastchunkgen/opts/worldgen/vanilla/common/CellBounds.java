package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.ICacheAllInCell;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.ICacheOnce;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.INoiseInterpolator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.core.Holder;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CellBounds {

    public static final int NORMAL = 0, AIR = 1, SOLID = 2;
    public static final double MARGIN = 1.0E-6D;

    public interface HighAir {
        boolean fcg$allHighAir(int minX, int minY, int minZ, int sizeXZ, int sizeY);

        // a skipped cell leaves it as vanillas last block would
        void fcg$skippedCell(boolean solid, int lastX, int lastY, int lastZ);
    }

    public interface BeardBounds {
        boolean fcg$zeroIn(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
    }

    public interface Cells {
        void fcg$beginCellSorting(boolean solidAllowed);

        int fcg$cellMode();

        boolean fcg$cellsFilled();

        boolean fcg$filling();

        int fcg$cellWidth();

        int fcg$cellHeight();

        BlockState fcg$interpolatedState();
    }

    public abstract static class Node {
        public double lo, hi;

        public abstract void eval(int cellY, int cellZ, int minX, int minY, int minZ);
    }

    private static final String DENSITY_FUNCTIONS = "net.minecraft.world.level.levelgen.DensityFunctions$";

    static final ClassValue<Field[]> COMPONENTS = new ClassValue<>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            final RecordComponent[] components = type.getRecordComponents();
            if (components == null) return new Field[0];
            final Field[] fields = new Field[components.length];
            int i = 0;
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (i == fields.length) break;
                if (field.getType() != components[i].getType()) return new Field[0];
                field.setAccessible(true);
                fields[i++] = field;
            }
            return i == fields.length ? fields : new Field[0];
        }
    };

    private static final String FLAT_CACHE = "net.minecraft.world.level.levelgen.NoiseChunk$FlatCache";
    private static final String SPECIAL_3D = "com.misanthropy.lithoform.bowl.Special3D";
    private static final MethodHandle PLAIN = plain();

    private static final Field[] UNKNOWN_BEARD_TERMS = new Field[0];
    private static final Set<String> KNOWN_BEARD_MIXINS = Set.of(
            "com.yungnickyoung.minecraft.yungsapi.mixin.BeardifierMixin",
            "com.craisinlord.integrated_api.mixins.structures.BeardifierMixin",
            "com.finndog.moogs_structures.mixins.terrainadaptation.BeardifierMixin",
            "com.finderfeed.fdbosses.mixin.BeardifierMixin",
            "io.redspace.ironsspellbooks.mixin.BeardifierMixin");
    private static final Set<String> VANILLA_BEARD_TERMS = Set.of("pieceIterator", "junctionIterator", "f_158065_", "f_158066_");
    private static volatile Field[] foreignBeardTerms;

    private static MethodHandle plain() {
        try {
            return MethodHandles.publicLookup().findStatic(Class.forName(SPECIAL_3D), "plain", MethodType.methodType(boolean.class, int.class, int.class, int.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static DensityFunction special(DensityFunction function) {
        try {
            return (DensityFunction) COMPONENTS.get(function.getClass())[0].get(function);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private CellBounds() {
    }

    public static boolean onlyVanillaBeardTerms(Beardifier beardifier) {
        Field[] terms = foreignBeardTerms;
        if (terms == null) terms = foreignBeardTerms = findForeignBeardTerms();
        if (terms == UNKNOWN_BEARD_TERMS) return false;
        try {
            for (Field term : terms) {
                final ObjectListIterator<?> iterator = (ObjectListIterator<?>) term.get(beardifier);
                if (iterator != null && (iterator.hasNext() || iterator.hasPrevious())) return false;
            }
            return true;
        } catch (IllegalAccessException | RuntimeException e) {
            return false;
        }
    }

    private static Field[] findForeignBeardTerms() {
        try {
            final Set<String> interfaces = new HashSet<>();
            for (Class<?> type : Beardifier.class.getInterfaces()) interfaces.add(type.getName());
            for (Method method : Beardifier.class.getDeclaredMethods()) {
                final MixinMerged merged = method.getAnnotation(MixinMerged.class);
                if (merged != null && !KNOWN_BEARD_MIXINS.contains(merged.mixin()) && !merged.mixin().startsWith("com.misanthropy.")
                        && !interfaces.contains(merged.mixin())) {
                    return UNKNOWN_BEARD_TERMS;
                }
            }
            final List<Field> terms = new ArrayList<>();
            for (Field field : Beardifier.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getName().startsWith("fcg$") || field.getName().startsWith("coo$")) continue;
                if (field.getType() != ObjectListIterator.class) return UNKNOWN_BEARD_TERMS;
                if (VANILLA_BEARD_TERMS.contains(field.getName())) continue;
                field.setAccessible(true);
                terms.add(field);
            }
            return terms.toArray(new Field[0]);
        } catch (Throwable t) {
            return UNKNOWN_BEARD_TERMS;
        }
    }

    public static Node compile(DensityFunction root, int cellWidth, int cellHeight, boolean emptyBlender) {
        try {
            final Node node = node(root, cellWidth, cellHeight, emptyBlender, 0);
            return node instanceof Fixed fixed && Double.isInfinite(fixed.lo) && Double.isInfinite(fixed.hi) ? null : node;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Node node(DensityFunction function, int width, int height, boolean emptyBlender, int depth) {
        if (depth > 32) return Fixed.unknown();
        if (function instanceof NoiseChunk.NoiseInterpolator) return new Corners((INoiseInterpolator) function);
        if (function instanceof ICacheAllInCell cache) return node(cache.fcg$noiseFiller(), width, height, emptyBlender, depth + 1);
        if (function instanceof ICacheOnce cache) return node(cache.fcg$function(), width, height, emptyBlender, depth + 1);
        if (function.getClass() == Beardifier.class) return new Beard((BeardBounds) function, width, height);
        if (function instanceof ColumnNoise) return new Column(function, width);
        if (function instanceof PointNoise point) return node(point.inner(), width, height, emptyBlender, depth + 1);
        final String name = function.getClass().getName();
        if (name.equals(FLAT_CACHE)) return new Column(function, width);
        if (name.equals(SPECIAL_3D)) {
            return PLAIN == null ? Fixed.unknown() : new Special(node(special(function), width, height, emptyBlender, depth + 1), width);
        }
        if (!name.startsWith(DENSITY_FUNCTIONS)) return Fixed.unknown();
        try {
            return record(function, name.substring(DENSITY_FUNCTIONS.length()), COMPONENTS.get(function.getClass()), width, height, emptyBlender, depth);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Fixed.unknown();
        }
    }

    @SuppressWarnings("unchecked")
    private static Node record(DensityFunction function, String type, Field[] parts, int width, int height, boolean emptyBlender, int depth) throws ReflectiveOperationException {
        switch (type) {
            case "BeardifierMarker":
                return new Fixed(0.0D, 0.0D);
            case "Constant": {
                final double value = (double) parts[0].get(function);
                return new Fixed(value, value);
            }
            case "YClampedGradient":
                return new YGradient((int) parts[0].get(function), (int) parts[1].get(function),
                        (double) parts[2].get(function), (double) parts[3].get(function), height);
            case "HolderHolder":
                return node(((Holder<DensityFunction>) parts[0].get(function)).value(), width, height, emptyBlender, depth + 1);
            case "BlendDensity":
                return emptyBlender ? node((DensityFunction) parts[0].get(function), width, height, emptyBlender, depth + 1) : Fixed.unknown();
            case "Ap2":
                return new Binary(((Enum<?>) parts[0].get(function)).ordinal(),
                        node((DensityFunction) parts[1].get(function), width, height, emptyBlender, depth + 1),
                        node((DensityFunction) parts[2].get(function), width, height, emptyBlender, depth + 1));
            case "MulOrAdd":
                return new MulOrAdd(((Enum<?>) parts[0].get(function)).ordinal() == 0, (double) parts[4].get(function),
                        node((DensityFunction) parts[1].get(function), width, height, emptyBlender, depth + 1));
            case "Mapped":
                return new Mapped(((Enum<?>) parts[0].get(function)).ordinal(),
                        node((DensityFunction) parts[1].get(function), width, height, emptyBlender, depth + 1));
            case "Clamp":
                return new Clamp((double) parts[1].get(function), (double) parts[2].get(function),
                        node((DensityFunction) parts[0].get(function), width, height, emptyBlender, depth + 1));
            case "RangeChoice":
                return new Choice((double) parts[1].get(function), (double) parts[2].get(function),
                        node((DensityFunction) parts[0].get(function), width, height, emptyBlender, depth + 1),
                        node((DensityFunction) parts[3].get(function), width, height, emptyBlender, depth + 1),
                        node((DensityFunction) parts[4].get(function), width, height, emptyBlender, depth + 1));
            case "Noise":
            case "ShiftedNoise":
            case "Shift":
                return new Fixed(function.minValue(), function.maxValue());
            case "Spline":
                return spline(((DensityFunctions.Spline) function).spline(), width, height, emptyBlender, depth + 1);
            default:
                return Fixed.unknown();
        }
    }

    private static Node spline(CubicSpline<?, ?> spline, int width, int height, boolean emptyBlender, int depth) {
        if (spline instanceof CubicSpline.Constant<?, ?> constant) return new Fixed(constant.value(), constant.value());
        if (depth > 32 || !(spline instanceof CubicSpline.Multipoint<?, ?> multipoint)
                || !(multipoint.coordinate() instanceof DensityFunctions.Spline.Coordinate coordinate)) return Fixed.unknown();
        final Node[] values = new Node[multipoint.values().size()];
        for (int i = 0; i < values.length; i++) values[i] = spline(multipoint.values().get(i), width, height, emptyBlender, depth + 1);
        return new SplineBounds(node(coordinate.function().value(), width, height, emptyBlender, depth + 1), multipoint.locations(), multipoint.derivatives(), values);
    }

    private static final class Column extends Node {
        private final DensityFunction function;
        private final int width;
        private int atX = Integer.MIN_VALUE, atZ = Integer.MIN_VALUE;

        Column(DensityFunction function, int width) {
            this.function = function;
            this.width = width;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            if (minX == this.atX && minZ == this.atZ) return;
            this.atX = minX;
            this.atZ = minZ;
            double lo = Double.POSITIVE_INFINITY;
            double hi = Double.NEGATIVE_INFINITY;
            for (int x = minX; x < minX + this.width; x++) {
                for (int z = minZ; z < minZ + this.width; z++) {
                    final double value = this.function.compute(new DensityFunction.SinglePointContext(x, minY, z));
                    lo = Math.min(lo, value);
                    hi = Math.max(hi, value);
                }
            }
            this.lo = lo;
            this.hi = hi;
        }
    }

    private static final class Special extends Node {
        private final Node argument;
        private final int width;
        private int atX = Integer.MIN_VALUE, atZ = Integer.MIN_VALUE;
        private boolean plain;

        Special(Node argument, int width) {
            this.argument = argument;
            this.width = width;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            if (minX != this.atX || minZ != this.atZ) {
                this.atX = minX;
                this.atZ = minZ;
                try {
                    this.plain = (boolean) PLAIN.invokeExact(minX, minZ, this.width);
                } catch (Throwable e) {
                    this.plain = false;
                }
            }
            if (this.plain) {
                this.argument.eval(cellY, cellZ, minX, minY, minZ);
                this.lo = this.argument.lo;
                this.hi = this.argument.hi;
            } else {
                this.lo = Double.NEGATIVE_INFINITY;
                this.hi = Double.POSITIVE_INFINITY;
            }
        }
    }

    private static final class SplineBounds extends Node {
        private final Node coordinate;
        private final float[] locations, derivatives;
        private final Node[] values;

        SplineBounds(Node coordinate, float[] locations, float[] derivatives, Node[] values) {
            this.coordinate = coordinate;
            this.locations = locations;
            this.derivatives = derivatives;
            this.values = values;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            this.coordinate.eval(cellY, cellZ, minX, minY, minZ);
            final double from = this.coordinate.lo, to = this.coordinate.hi;
            final int last = this.locations.length - 1;
            this.lo = Double.POSITIVE_INFINITY;
            this.hi = Double.NEGATIVE_INFINITY;
            if (!(from <= to)) {
                this.lo = Double.NEGATIVE_INFINITY;
                this.hi = Double.POSITIVE_INFINITY;
                return;
            }
            if (from < this.locations[0]) this.end(0, from, Math.min(to, this.locations[0]), cellY, cellZ, minX, minY, minZ);
            if (to >= this.locations[last]) this.end(last, Math.max(from, this.locations[last]), to, cellY, cellZ, minX, minY, minZ);
            for (int i = 0; i < last; i++) {
                if (to < this.locations[i] || from > this.locations[i + 1]) continue;
                final Node a = this.values[i], b = this.values[i + 1];
                a.eval(cellY, cellZ, minX, minY, minZ);
                b.eval(cellY, cellZ, minX, minY, minZ);
                double lo = Math.min(a.lo, b.lo), hi = Math.max(a.hi, b.hi);
                final float derivativeA = this.derivatives[i], derivativeB = this.derivatives[i + 1];
                if (derivativeA != 0.0F || derivativeB != 0.0F) {
                    final double span = this.locations[i + 1] - this.locations[i];
                    final double slopeA = derivativeA * span, slopeB = derivativeB * span;
                    lo += 0.25D * Math.min(Math.min(slopeA - b.hi + a.lo, -slopeB + b.lo - a.hi), 0.0D);
                    hi += 0.25D * Math.max(Math.max(slopeA - b.lo + a.hi, -slopeB + b.hi - a.lo), 0.0D);
                }
                this.lo = Math.min(this.lo, lo);
                this.hi = Math.max(this.hi, hi);
            }
            final double slack = 1.0E-5D * (1.0D + Math.max(Math.abs(this.lo), Math.abs(this.hi)));
            this.lo -= slack;
            this.hi += slack;
        }

        private void end(int i, double from, double to, int cellY, int cellZ, int minX, int minY, int minZ) {
            this.values[i].eval(cellY, cellZ, minX, minY, minZ);
            double lo = this.values[i].lo, hi = this.values[i].hi;
            final double derivative = this.derivatives[i];
            if (derivative != 0.0D) {
                final double a = derivative * (from - this.locations[i]), b = derivative * (to - this.locations[i]);
                lo += Math.min(a, b);
                hi += Math.max(a, b);
            }
            this.lo = Math.min(this.lo, lo);
            this.hi = Math.max(this.hi, hi);
        }
    }

    private static final class Fixed extends Node {
        Fixed(double lo, double hi) {
            this.lo = lo;
            this.hi = hi;
        }

        static Fixed unknown() {
            return new Fixed(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
        }
    }

    private static final class Corners extends Node {
        private final INoiseInterpolator interpolator;

        Corners(INoiseInterpolator interpolator) {
            this.interpolator = interpolator;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            final double[][] slice0 = this.interpolator.fcg$slice0();
            final double[][] slice1 = this.interpolator.fcg$slice1();
            double lo = Double.POSITIVE_INFINITY;
            double hi = Double.NEGATIVE_INFINITY;
            for (int dz = 0; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    final double a = slice0[cellZ + dz][cellY + dy];
                    final double b = slice1[cellZ + dz][cellY + dy];
                    lo = Math.min(lo, Math.min(a, b));
                    hi = Math.max(hi, Math.max(a, b));
                }
            }
            this.lo = lo;
            this.hi = hi;
        }
    }

    private static final class Beard extends Node {
        private final BeardBounds beard;
        private final int width, height;

        Beard(BeardBounds beard, int width, int height) {
            this.beard = beard;
            this.width = width;
            this.height = height;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            final boolean zero = this.beard.fcg$zeroIn(minX, minY, minZ, minX + this.width - 1, minY + this.height - 1, minZ + this.width - 1);
            this.lo = zero ? 0.0D : Double.NEGATIVE_INFINITY;
            this.hi = zero ? 0.0D : Double.POSITIVE_INFINITY;
        }
    }

    private static final class YGradient extends Node {
        private final int fromY, toY, height;
        private final double fromValue, toValue;

        YGradient(int fromY, int toY, double fromValue, double toValue, int height) {
            this.fromY = fromY;
            this.toY = toY;
            this.fromValue = fromValue;
            this.toValue = toValue;
            this.height = height;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            final double bottom = Mth.clampedMap(minY, this.fromY, this.toY, this.fromValue, this.toValue);
            final double top = Mth.clampedMap(minY + this.height - 1, this.fromY, this.toY, this.fromValue, this.toValue);
            this.lo = Math.min(bottom, top);
            this.hi = Math.max(bottom, top);
        }
    }

    private static final class Binary extends Node {
        private final int type;
        private final Node first, second;

        Binary(int type, Node first, Node second) {
            this.type = type;
            this.first = first;
            this.second = second;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            this.first.eval(cellY, cellZ, minX, minY, minZ);
            this.second.eval(cellY, cellZ, minX, minY, minZ);
            final double lo1 = this.first.lo, hi1 = this.first.hi, lo2 = this.second.lo, hi2 = this.second.hi;
            switch (this.type) {
                case 0 -> {
                    this.lo = lo1 + lo2;
                    this.hi = hi1 + hi2;
                }
                case 1 -> {
                    final double a = lo1 * lo2, b = lo1 * hi2, c = hi1 * lo2, d = hi1 * hi2;
                    this.lo = Math.min(Math.min(a, b), Math.min(c, d));
                    this.hi = Math.max(Math.max(a, b), Math.max(c, d));
                }
                case 2 -> {
                    this.lo = Math.min(lo1, lo2);
                    this.hi = Math.min(hi1, hi2);
                }
                default -> {
                    this.lo = Math.max(lo1, lo2);
                    this.hi = Math.max(hi1, hi2);
                }
            }
        }
    }

    private static final class MulOrAdd extends Node {
        private final boolean mul;
        private final double argument;
        private final Node input;

        MulOrAdd(boolean mul, double argument, Node input) {
            this.mul = mul;
            this.argument = argument;
            this.input = input;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            this.input.eval(cellY, cellZ, minX, minY, minZ);
            if (!this.mul) {
                this.lo = this.input.lo + this.argument;
                this.hi = this.input.hi + this.argument;
            } else if (this.argument >= 0.0D) {
                this.lo = this.input.lo * this.argument;
                this.hi = this.input.hi * this.argument;
            } else {
                this.lo = this.input.hi * this.argument;
                this.hi = this.input.lo * this.argument;
            }
        }
    }

    private static final class Mapped extends Node {
        private final int type;
        private final Node input;

        Mapped(int type, Node input) {
            this.type = type;
            this.input = input;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            this.input.eval(cellY, cellZ, minX, minY, minZ);
            final double lo = this.input.lo, hi = this.input.hi;
            if (this.type <= 1) {
                final double absLo = lo >= 0.0D ? lo : hi <= 0.0D ? -hi : 0.0D;
                final double absHi = Math.max(-lo, hi);
                this.lo = this.type == 0 ? absLo : absLo * absLo;
                this.hi = this.type == 0 ? absHi : absHi * absHi;
                if (Double.isNaN(lo) || Double.isNaN(hi)) this.lo = this.hi = Double.NaN;
            } else {
                this.lo = transform(this.type, lo);
                this.hi = transform(this.type, hi);
            }
        }

        private static double transform(int type, double d) {
            return switch (type) {
                case 2 -> d * d * d;
                case 3 -> d > 0.0D ? d : d * 0.5D;
                case 4 -> d > 0.0D ? d : d * 0.25D;
                default -> {
                    final double e = Mth.clamp(d, -1.0D, 1.0D);
                    yield e / 2.0D - e * e * e / 24.0D;
                }
            };
        }
    }

    private static final class Clamp extends Node {
        private final double min, max;
        private final Node input;

        Clamp(double min, double max, Node input) {
            this.min = min;
            this.max = max;
            this.input = input;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            this.input.eval(cellY, cellZ, minX, minY, minZ);
            this.lo = Mth.clamp(this.input.lo, this.min, this.max);
            this.hi = Mth.clamp(this.input.hi, this.min, this.max);
        }
    }

    private static final class Choice extends Node {
        private final double minInclusive, maxExclusive;
        private final Node input, inRange, outOfRange;

        Choice(double minInclusive, double maxExclusive, Node input, Node inRange, Node outOfRange) {
            this.minInclusive = minInclusive;
            this.maxExclusive = maxExclusive;
            this.input = input;
            this.inRange = inRange;
            this.outOfRange = outOfRange;
        }

        @Override
        public void eval(int cellY, int cellZ, int minX, int minY, int minZ) {
            this.input.eval(cellY, cellZ, minX, minY, minZ);
            final double lo = this.input.lo, hi = this.input.hi;
            if (lo >= this.minInclusive && hi < this.maxExclusive) {
                this.inRange.eval(cellY, cellZ, minX, minY, minZ);
                this.lo = this.inRange.lo;
                this.hi = this.inRange.hi;
            } else if (hi < this.minInclusive || lo >= this.maxExclusive) {
                this.outOfRange.eval(cellY, cellZ, minX, minY, minZ);
                this.lo = this.outOfRange.lo;
                this.hi = this.outOfRange.hi;
            } else {
                this.inRange.eval(cellY, cellZ, minX, minY, minZ);
                this.outOfRange.eval(cellY, cellZ, minX, minY, minZ);
                this.lo = Math.min(this.inRange.lo, this.outOfRange.lo);
                this.hi = Math.max(this.inRange.hi, this.outOfRange.hi);
            }
        }
    }

}
