package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public final class ShortcutVerify {

    public static final boolean ENABLED = Boolean.getBoolean("fastchunkgen.verifyShortcuts");
    private static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Verify");

    private final String name;
    private final AtomicLong checked = new AtomicLong();
    private final AtomicLong mismatches = new AtomicLong();
    private volatile boolean broken;

    public static final ShortcutVerify FLUID = new ShortcutVerify("still fluid");
    public static final ShortcutVerify CLIMATE = new ShortcutVerify("flat climate");
    public static final ShortcutVerify COLUMN = new ShortcutVerify("column climate");
    public static final ShortcutVerify COLUMN_NOISE = new ShortcutVerify("column noise");
    public static final ShortcutVerify SKIPPED_CELLS = new ShortcutVerify("skipped cells");
    public static final ShortcutVerify PROBED_CELLS = new ShortcutVerify("probed cells");
    public static final ShortcutVerify SHAPES = new ShortcutVerify("static shapes");
    public static final ShortcutVerify SET_BLOCK = new ShortcutVerify("redundant setBlock");
    public static final ShortcutVerify FLAT_COLUMNS = new ShortcutVerify("flat cache columns");
    public static final ShortcutVerify SURFACE_FOLD = new ShortcutVerify("surface rule fold");
    public static final ShortcutVerify BLOCK_STATES = new ShortcutVerify("direct block states");

    private ShortcutVerify(String name) {
        this.name = name;
    }

    public boolean broken() {
        return this.broken;
    }

    public void broke(Object detail) {
        if (!this.broken) {
            this.broken = true;
            LOGGER.error("[{}] gave a different answer than the original at {}, switched off for the rest of this run", this.name, detail);
        }
    }

    public static boolean due(int count) {
        return (count & 4095) == 0;
    }

    public void record(boolean ok, Object detail) {
        final long n = this.checked.incrementAndGet();
        if (!ok && this.mismatches.incrementAndGet() <= 20) {
            LOGGER.error("[{}] MISMATCH {}", this.name, detail);
        }
        if ((n & (n - 1)) == 0 || (n & ((1 << 20) - 1)) == 0) {
            LOGGER.info("[{}] checked={} mismatches={}", this.name, n, this.mismatches.get());
        }
    }
}
