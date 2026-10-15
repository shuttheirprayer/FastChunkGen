package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.ICacheAllInCell;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.ICacheOnce;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.INoiseInterpolator;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.HashSet;
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

    private static volatile Boolean onlyVanillaBeardTerms;

    private CellBounds() {
    }

    public static boolean onlyVanillaBeardTerms() {
        Boolean only = onlyVanillaBeardTerms;
        if (only == null) {
            only = true;
            try {
                final Set<String> interfaces = new HashSet<>();
                for (Class<?> type : Beardifier.class.getInterfaces()) interfaces.add(type.getName());
                for (Method method : Beardifier.class.getDeclaredMethods()) {
                    final MixinMerged merged = method.getAnnotation(MixinMerged.class);
                    if (merged != null && !merged.mixin().startsWith("com.misanthropy.fastchunkgen.") && !interfaces.contains(merged.mixin())) only = false;
                }
            } catch (Throwable t) {
                only = false;
            }
            onlyVanillaBeardTerms = only;
        }
        return only;
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
        final String name = function.getClass().getName();
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
            default:
                return Fixed.unknown();
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
