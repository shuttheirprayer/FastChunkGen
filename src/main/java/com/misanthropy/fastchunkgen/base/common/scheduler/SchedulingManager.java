package com.misanthropy.fastchunkgen.base.common.scheduler;

import com.misanthropy.fastchunkgen.base.common.structs.DynamicPriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Scheduling Manager");

    public static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final DynamicPriorityQueue<ScheduledTask> queue = new DynamicPriorityQueue<>(MAX_LEVEL + 1);
    private final Long2ReferenceOpenHashMap<ObjectArraySet<ScheduledTask>> pos2Tasks = new Long2ReferenceOpenHashMap<>();
    private final Long2IntOpenHashMap prioritiesFromLevel = new Long2IntOpenHashMap();
    private final NeighborLockingManager neighborLockingManager = new NeighborLockingManager();
    private final AtomicInteger scheduledCount = new AtomicInteger(0);
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private ChunkPos currentSyncLoad = null;

    private final Executor executor;
    private final int maxScheduled;

    {
        prioritiesFromLevel.defaultReturnValue(MAX_LEVEL);
    }

    public SchedulingManager(Executor executor, int maxScheduled) {
        this.executor = executor;
        this.maxScheduled = maxScheduled;
    }

    private void submitGuarded(Runnable runnable) {
        this.executor.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOGGER.error("Error while running chunk scheduling task", t);
            }
        });
    }

    public void enqueue(ScheduledTask task) {
        submitGuarded(() -> {
            if (task.isAsync()) {
                schedule0(task);
            } else {
                final long pos = task.centerPos();
                queue.enqueue(task, prioritiesFromLevel.get(pos));
                pos2Tasks.computeIfAbsent(pos, unused -> new ObjectArraySet<>()).add(task);
                scheduleExecution();
            }
        });
    }

    public void updatePriorityFromLevel(long pos, int level) {
        submitGuarded(() -> {
            if (prioritiesFromLevel.get(pos) == level) return;
            if (level < MAX_LEVEL) {
                prioritiesFromLevel.put(pos, level);
            } else {
                prioritiesFromLevel.remove(pos);
            }
            updatePriorityInternal(pos);
        });
    }

    private void updatePriorityInternal(long pos) {
        final ObjectArraySet<ScheduledTask> locks = this.pos2Tasks.get(pos);
        if (locks == null || locks.isEmpty()) return;

        int fromSyncLoad = MAX_LEVEL;
        if (currentSyncLoad != null) {
            final int chebyshevDistance = chebyshev(pos, currentSyncLoad.toLong());
            if (chebyshevDistance <= 8) fromSyncLoad = chebyshevDistance;
        }
        final int priority = Math.min(prioritiesFromLevel.get(pos), fromSyncLoad);
        for (ScheduledTask lock : locks) {
            queue.changePriority(lock, priority);
        }
    }

    public void setCurrentSyncLoad(ChunkPos pos) {
        submitGuarded(() -> {
            if (this.currentSyncLoad != null) {
                final ChunkPos lastSyncLoad = this.currentSyncLoad;
                this.currentSyncLoad = null;
                updateSyncLoadInternal(lastSyncLoad);
            }
            if (pos != null) {
                this.currentSyncLoad = pos;
                updateSyncLoadInternal(pos);
            }
        });
    }

    public NeighborLockingManager getNeighborLockingManager() {
        return this.neighborLockingManager;
    }

    public Executor getExecutor() {
        return executor;
    }

    private void updateSyncLoadInternal(ChunkPos pos) {
        for (int xOff = -8; xOff <= 8; xOff++) {
            for (int zOff = -8; zOff <= 8; zOff++) {
                updatePriorityInternal(ChunkPos.asLong(pos.x + xOff, pos.z + zOff));
            }
        }
    }

    private void scheduleExecution() {
        if (scheduledCount.get() < maxScheduled && scheduled.compareAndSet(false, true)) {
            this.executor.execute(() -> {
                try {
                    while (scheduledCount.get() < maxScheduled && scheduleExecutionInternal()) {
                        scheduledCount.incrementAndGet();
                    }
                } catch (Throwable t) {
                    LOGGER.error("Error while scheduling chunk tasks", t);
                } finally {
                    scheduled.set(false);
                }
            });
        }
    }

    private boolean scheduleExecutionInternal() {
        final ScheduledTask task = queue.dequeue();
        if (task == null) return false;

        final long pos = task.centerPos();
        final ObjectArraySet<ScheduledTask> tasks = this.pos2Tasks.get(pos);
        if (tasks != null) {
            tasks.remove(task);
            if (tasks.isEmpty()) this.pos2Tasks.remove(pos);
        }
        try {
            return schedule0(task);
        } catch (Throwable t) {
            LOGGER.error("Error while preparing chunk task at {}", new ChunkPos(pos), t);
            return false;
        }
    }

    private boolean schedule0(ScheduledTask task) {
        if (task.tryPrepare()) {
            task.runTask(() -> {
                scheduledCount.decrementAndGet();
                scheduleExecution();
            });
            return true;
        }
        return false;
    }

    private static int chebyshev(long a, long b) {
        return Math.max(Math.abs(ChunkPos.getX(a) - ChunkPos.getX(b)), Math.abs(ChunkPos.getZ(a) - ChunkPos.getZ(b)));
    }

}
