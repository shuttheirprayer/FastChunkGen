package com.misanthropy.fastchunkgen.base.common.scheduler;

import com.misanthropy.fastchunkgen.base.common.structs.SimpleObjectPool;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Not thread safe.
 */
public class NeighborLockingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("NeighborLockingManager");

    private final SimpleObjectPool<ReferenceArraySet<Runnable>> pool = new SimpleObjectPool<>(
            pool -> new ReferenceArraySet<>(16),
            ReferenceArraySet::clear,
            1024
    );
    private final Long2ReferenceOpenHashMap<ReferenceArraySet<Runnable>> activeLocks = new Long2ReferenceOpenHashMap<>();

    public boolean isLocked(long pos) {
        return activeLocks.containsKey(pos);
    }

    public void acquireLock(long pos) {
        if (isLocked(pos)) throw new IllegalStateException("Already locked");
        activeLocks.put(pos, pool.alloc());
    }

    public void releaseLock(long pos) {
        final ReferenceArraySet<Runnable> runnables = activeLocks.remove(pos);
        if (runnables == null) throw new IllegalStateException("Not locked");
        for (Runnable runnable : runnables) {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOGGER.error("Failed to notify lock release at chunk %s".formatted(new ChunkPos(pos)), t);
            }
        }
        runnables.clear();
        pool.release(runnables);
    }

    public void addReleaseListener(long pos, Runnable runnable) {
        final ReferenceArraySet<Runnable> runnables = activeLocks.get(pos);
        if (runnables == null) throw new IllegalStateException("Not locked");
        runnables.add(runnable);
    }

}
