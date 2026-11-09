package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common;

import com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.debug.SMAPSourceDebugExtension;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class CheckedThreadLocalRandom extends SingleThreadedRandomSource {

    private static final Logger LOGGER = LoggerFactory.getLogger("CheckedThreadLocalRandom");

    private static final ThreadLocal<SingleThreadedRandomSource> FALLBACK = ThreadLocal.withInitial(() -> new SingleThreadedRandomSource(new Random().nextLong()));

    static {
        if (Config.enforceSafeWorldRandomAccess) {
            LOGGER.info("Enforcing safe world random access");
        } else {
            LOGGER.warn("Not enforcing safe world random access");
        }
    }

    private static final long REPORT_INTERVAL_NANOS = 60L * 1000L * 1000L * 1000L;

    private static final AtomicLong lastReportNanos = new AtomicLong(Long.MIN_VALUE);

    private static final AtomicLong suppressedReports = new AtomicLong();

    private static boolean shouldReport() {
        final long now = System.nanoTime();
        while (true) {
            final long last = lastReportNanos.get();
            if (last != Long.MIN_VALUE && now - last < REPORT_INTERVAL_NANOS) {
                suppressedReports.incrementAndGet();
                return false;
            }
            if (lastReportNanos.compareAndSet(last, now)) {
                final long suppressed = suppressedReports.getAndSet(0L);
                if (suppressed > 0L) {
                    LOGGER.error("Suppressed {} further unsafe world random access reports in the last minute", suppressed);
                }
                return true;
            }
        }
    }

    private final Supplier<Thread> owner;

    public CheckedThreadLocalRandom(long seed, Supplier<Thread> owner) {
        super(seed);
        this.owner = Objects.requireNonNull(owner);
    }

    private boolean isSafe() {
        Thread owner = this.owner != null ? this.owner.get() : null;
        boolean notOwner = owner != null && Thread.currentThread() != owner;
        if (notOwner) {
            handleNotOwner();
            return false;
        } else {
            return true;
        }
    }

    private void handleNotOwner() {
        if (!Config.enforceSafeWorldRandomAccess && !shouldReport()) {
            return;
        }
        final Thread ownerThread = this.owner.get();
        StringBuilder builder = new StringBuilder();
        final String exceptionMessage = "ThreadLocalRandom accessed from a different thread (owner: %s, current: %s)"
                .formatted(ownerThread != null ? ownerThread.getName() : "<unknown>", Thread.currentThread().getName());
        builder.append(exceptionMessage).append('\n');
        builder.append("This is usually NOT a bug in FastChunkGen, but a bug in another mod or in vanilla code. \n");
        builder.append("Possible solutions: \n");
        builder.append("  - Find possible causes in the stack trace below and \n");
        builder.append("    - if caused by another mod, report this to the corresponding mod authors \n");
        builder.append("    - if no other mods are involved, report this to FastChunkGen \n");
        ConcurrentModificationException exception = new ConcurrentModificationException(exceptionMessage);
        try {
            SMAPSourceDebugExtension.enhanceStackTrace(exception, false);
        } catch (Throwable t) {
            LOGGER.error("Error occurred while processing error stack trace", t);
            exception = new ConcurrentModificationException(exceptionMessage);
        }

        final String s = builder.toString();
        LOGGER.error(s, exception);
        if (Config.enforceSafeWorldRandomAccess) {
            throw new RuntimeException(String.format("%s \n (You may make this a fatal warning instead of a hard crash with fixes.enforceSafeWorldRandomAccess setting in fastchunkgen.toml)", s), exception) {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            };
        }
    }

    @Override
    public void setSeed(long seed) {
        if (isSafe()) {
            super.setSeed(seed);
        } else {
            FALLBACK.get().setSeed(seed);
        }
    }

    @Override
    public int next(int bits) {
        if (isSafe()) {
            return super.next(bits);
        } else {
            return FALLBACK.get().next(bits);
        }
    }
}
