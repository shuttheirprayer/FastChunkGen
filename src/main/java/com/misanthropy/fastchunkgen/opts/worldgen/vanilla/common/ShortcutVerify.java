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

    public static final ShortcutVerify FLUID = new ShortcutVerify("still fluid");
    public static final ShortcutVerify CLIMATE = new ShortcutVerify("flat climate");
    public static final ShortcutVerify COLUMN = new ShortcutVerify("column climate");
    public static final ShortcutVerify SHAPES = new ShortcutVerify("static shapes");
    public static final ShortcutVerify BLOCK_STATES = new ShortcutVerify("direct block states");

    private ShortcutVerify(String name) {
        this.name = name;
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
