package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import com.misanthropy.fastchunkgen.base.common.util.Log;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class FlatColumnMemos {

    public interface Chunk {
        int fcg$nextFlatIndex();

        FlatColumnMemos fcg$flatColumnMemos();
    }

    private static final int MAX_CACHES = 64;
    private static final int SLOTS = 1 << 12;
    private static final int CANARY = 256;
    private static final int CHECK_EVERY = 4096;
    private static final VarHandle CELLS = MethodHandles.arrayElementVarHandle(long[].class);
    private static final Map<RandomState, FlatColumnMemos> BY_STATE = Collections.synchronizedMap(new WeakHashMap<>());
    public static final Memo NONE = new Memo(null, null, false, 0L, 0L);

    private final AtomicReferenceArray<Memo> memos = new AtomicReferenceArray<>(MAX_CACHES);
    private final AtomicInteger canary = new AtomicInteger(CANARY);
    private volatile boolean ok = true;

    private FlatColumnMemos() {
    }

    public static FlatColumnMemos of(RandomState state) {
        FlatColumnMemos memos = BY_STATE.get(state);
        if (memos == null) {
            memos = BY_STATE.computeIfAbsent(state, unused -> new FlatColumnMemos());
        }
        return memos.ok ? memos : null;
    }

    public Memo memo(int index, DensityFunction wrapped) {
        if (index >= MAX_CACHES) return NONE;
        Memo memo = this.memos.get(index);
        if (memo == null) {
            final Memo made = new Memo(this, wrapped.getClass(), SharedNoise.isYFree(wrapped),
                    Double.doubleToRawLongBits(wrapped.minValue()), Double.doubleToRawLongBits(wrapped.maxValue()));
            memo = this.memos.compareAndSet(index, null, made) ? made : this.memos.get(index);
        }
        return memo.matches(wrapped) ? memo : NONE;
    }

    private boolean shouldCheck(Memo memo) {
        if (ShortcutVerify.ENABLED) return true;
        if (this.canary.get() > 0 && this.canary.decrementAndGet() >= 0) return true;
        return (++memo.served & (CHECK_EVERY - 1)) == 0;
    }

    private double checked(double remembered, double computed, int x, int z) {
        final boolean same = Double.doubleToRawLongBits(remembered) == Double.doubleToRawLongBits(computed);
        if (ShortcutVerify.ENABLED) ShortcutVerify.FLAT_COLUMNS.record(same, x + "," + z);
        if (!same && this.ok) {
            this.ok = false;
            Log.MAIN.error("Flat cache column memo switched off for one world generator: a remembered column at {},{} differed from the original evaluation ({} vs {})", x, z, remembered, computed);
        }
        return computed;
    }

    public static final class Memo {

        private final FlatColumnMemos owner;
        private final Class<?> type;
        private final boolean usable;
        private final long min;
        private final long max;
        private volatile long[] cells;
        private int served;

        private Memo(FlatColumnMemos owner, Class<?> type, boolean usable, long min, long max) {
            this.owner = owner;
            this.type = type;
            this.usable = usable;
            this.min = min;
            this.max = max;
        }

        private boolean matches(DensityFunction wrapped) {
            return this.usable && wrapped.getClass() == this.type
                    && Double.doubleToRawLongBits(wrapped.minValue()) == this.min
                    && Double.doubleToRawLongBits(wrapped.maxValue()) == this.max;
        }

        public boolean active() {
            return this.owner != null && this.owner.ok;
        }

        public double find(int x, int z) {
            final long[] cells = this.cells;
            if (cells == null) return Double.NaN;
            final int base = slot(x, z);
            final long before = (long) CELLS.getVolatile(cells, base);
            if (before == 0L || (before & 1L) != 0L) return Double.NaN;
            final long storedKey = (long) CELLS.getVolatile(cells, base + 1);
            final long bits = (long) CELLS.getVolatile(cells, base + 2);
            return (long) CELLS.getVolatile(cells, base) == before && storedKey == key(x, z) ? Double.longBitsToDouble(bits) : Double.NaN;
        }

        public void put(int x, int z, double value) {
            if (Double.isNaN(value)) return;
            long[] cells = this.cells;
            if (cells == null) {
                cells = new long[SLOTS * 3];
                this.cells = cells;
            }
            final int base = slot(x, z);
            final long before = (long) CELLS.getVolatile(cells, base);
            if ((before & 1L) == 0L && CELLS.compareAndSet(cells, base, before, before | 1L)) {
                CELLS.setVolatile(cells, base + 1, key(x, z));
                CELLS.setVolatile(cells, base + 2, Double.doubleToRawLongBits(value));
                CELLS.setVolatile(cells, base, (before | 1L) + 1L);
            }
        }

        public boolean shouldCheck() {
            return this.owner.shouldCheck(this);
        }

        public double checked(double remembered, double computed, int x, int z) {
            return this.owner.checked(remembered, computed, x, z);
        }

        private static long key(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }

        private static int slot(int x, int z) {
            final int hash = x * -1640531535 + z * -2048144789;
            return ((hash ^ hash >>> 15) & (SLOTS - 1)) * 3;
        }
    }
}
