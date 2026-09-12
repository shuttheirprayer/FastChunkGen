package com.misanthropy.fastchunkgen.rewrites.chunkio.common;

import com.ibm.asyncutil.util.Either;
import com.misanthropy.fastchunkgen.base.common.GlobalExecutors;
import com.misanthropy.fastchunkgen.base.common.structs.RawByteArrayOutputStream;
import com.misanthropy.fastchunkgen.base.common.util.RegionFileUtil;
import com.misanthropy.fastchunkgen.base.common.util.SneakyThrow;
import com.misanthropy.fastchunkgen.base.mixin.access.IRegionFile;
import com.misanthropy.fastchunkgen.opts.chunkio.common.ConfigConstants;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public class C2MEStorageThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Storage");

    private static final AtomicLong SERIAL = new AtomicLong(0);

    private static final boolean DEBUG = Boolean.getBoolean("fastchunkgen.chunkio.debug");

    private static final int SPIN_ITERATIONS = 200;

    private static final long SPIN_PARK_NANOS = 50_000L;

    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private final RegionFileStorage storage;
    private final Long2ReferenceLinkedOpenHashMap<Either<CompoundTag, byte[]>> writeBacklog = new Long2ReferenceLinkedOpenHashMap<>();
    private final Long2ReferenceLinkedOpenHashMap<Either<CompoundTag, byte[]>> cache = new Long2ReferenceLinkedOpenHashMap<>();
    private final ConcurrentLinkedQueue<ReadRequest> pendingReadRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<WriteRequest> pendingWriteRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();
    private final Executor executor = command -> {
        if (Thread.currentThread() == this) {
            command.run();
        } else {
            pendingTasks.add(command);
            this.wakeUp();
        }
    };
    private final ObjectOpenHashSet<CompletableFuture<Void>> writeFutures = new ObjectOpenHashSet<>();
    private final Long2ObjectOpenHashMap<ObjectArrayList<CompletableFuture<Void>>> writeCompletions = new Long2ObjectOpenHashMap<>();
    private static final Either<CompoundTag, byte[]> CACHE_MISS = Either.right(new byte[0]);
    private final Object sync = new Object();
    private volatile boolean waiting = false;

    public C2MEStorageThread(Path directory, boolean dsync, String name) {
        this.storage = new RegionFileStorage(directory, dsync);
        this.setName("FastChunkGen Storage #%d".formatted(SERIAL.incrementAndGet()));
        this.setDaemon(true);
        this.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Thread %s died".formatted(t), e));
        this.start();
    }

    @Override
    public void run() {
        main_loop:
        while (true) {
            boolean hasWork = false;
            hasWork |= pollTasksSafely();

            runWriteFutureGC();

            if (!hasWork) {
                if (this.closing.get()) {
                    try {
                        flush0(true);
                    } catch (Throwable t) {
                        LOGGER.error("Error flushing storage on close", t);
                    }
                    try {
                        this.storage.close();
                    } catch (Throwable t) {
                        LOGGER.error("Error closing storage", t);
                    }
                    failRemainingRequests();
                    this.closeFuture.complete(null);
                    break;
                } else {
                    // attempt to spin-wait before sleeping
                    if (!pollTasksSafely()) {
                        Thread.interrupted(); // clear interrupt flag
                        for (int i = 0; i < SPIN_ITERATIONS; i ++) {
                            if (pollTasksSafely()) continue main_loop;
                            LockSupport.parkNanos("Spin-waiting for tasks", SPIN_PARK_NANOS);
                        }
                    }
                    synchronized (sync) {
                        this.waiting = true;
                        try {
                            if (this.hasPendingTasks() || this.closing.get()) continue main_loop;
                            sync.wait();
                        } catch (InterruptedException ignored) {
                        } finally {
                            this.waiting = false;
                        }
                    }
                }
            }
        }
        LOGGER.info("Storage thread {} stopped", this);
    }

    private boolean pollTasksSafely() {
        try {
            return pollTasks();
        } catch (Throwable t) {
            LOGGER.error("Error while processing storage tasks", t);
            return false;
        }
    }

    private boolean pollTasks() {
        boolean hasWork = false;
        hasWork = handleTasks() || hasWork;
        hasWork = handlePendingWrites() || hasWork;
        hasWork = handlePendingReads() || hasWork;
        hasWork = writeBacklog() || hasWork;
        return hasWork;
    }

    private boolean hasPendingTasks() {
        return !this.pendingTasks.isEmpty() || !this.pendingReadRequests.isEmpty() || !this.pendingWriteRequests.isEmpty() || !this.writeBacklog.isEmpty();
    }

    private void wakeUp() {
        if (!this.waiting) return;
        synchronized (sync) {
            sync.notifyAll();
        }
    }

    /**
     * Read chunk data from storage
     * @param pos target pos
     * @param scanner if null then ignored, if non-null then used and produce null future
     * @return future
     */
    public CompletableFuture<CompoundTag> getChunkData(long pos, StreamTagVisitor scanner) {
        final CompletableFuture<CompoundTag> future = new CompletableFuture<>();
        if (this.closing.get()) {
            future.completeExceptionally(new CancellationException());
            return future.thenApply(Function.identity());
        }
        this.pendingReadRequests.add(new ReadRequest(pos, future, scanner));
        this.wakeUp();
        if (DEBUG) {
            future.thenApply(Function.identity()).orTimeout(60, TimeUnit.SECONDS).exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    LOGGER.warn("Chunk read at {} took longer than a minute", new ChunkPos(pos));
                }
                return null;
            });
        }
        return future
                .thenApply(Function.identity());
    }

    public CompletableFuture<Void> setChunkData(long pos, @Nullable CompoundTag nbt) {
        return setChunkData0(pos, nbt != null ? Either.left(nbt) : null);
    }

    public CompletableFuture<Void> setChunkData(long pos, @Nullable byte[] data) {
        return setChunkData0(pos, data != null ? Either.right(data) : null);
    }

    private CompletableFuture<Void> setChunkData0(long pos, @Nullable Either<CompoundTag, byte[]> data) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        if (this.closing.get()) {
            future.completeExceptionally(new CancellationException("Storage is closing, write for %s dropped".formatted(new ChunkPos(pos))));
            return future;
        }
        this.pendingWriteRequests.add(new WriteRequest(pos, data, future));
        this.wakeUp();
        return future;
    }

    public CompletableFuture<Void> flush(boolean sync) {
        if (this.closeFuture.isDone()) return this.closeFuture.thenApply(Function.identity());
        return CompletableFuture.runAsync(() -> flush0(sync), this.executor);
    }

    private void flush0(boolean sync) {
        while (true) {
            runWriteFutureGC();
            if (handleTasks()) continue;
            if (handlePendingReads()) continue;
            if (handlePendingWrites()) continue;
            if (writeBacklog()) continue;

            break;
        }
        flushBacklog();
        if (sync) {
            try {
                this.storage.flush();
            } catch (Throwable t) {
                throw new RuntimeException("Error flushing storage", t);
            }
        }
    }

    public CompletableFuture<Void> close() {
        this.closing.set(true);
        this.wakeUp();
        return this.closeFuture.thenApply(Function.identity());
    }

    private void failRemainingRequests() {
        ReadRequest readRequest;
        while ((readRequest = this.pendingReadRequests.poll()) != null) {
            readRequest.future.completeExceptionally(new CancellationException("Storage closed before read completed"));
        }
        WriteRequest writeRequest;
        while ((writeRequest = this.pendingWriteRequests.poll()) != null) {
            LOGGER.error("Chunk {} was queued for writing after storage close, data is lost", new ChunkPos(writeRequest.pos));
            writeRequest.future.completeExceptionally(new CancellationException("Storage closed before write completed"));
        }
        for (long pos : this.writeCompletions.keySet().toLongArray()) {
            LOGGER.error("Chunk {} was still pending when storage closed, data is lost", new ChunkPos(pos));
            completeWriteFutures(pos, new CancellationException("Storage closed before write completed"));
        }
    }

    private boolean handleTasks() {
        boolean hasWork = false;
        Runnable runnable;
        while ((runnable = this.pendingTasks.poll()) != null) {
            hasWork = true;
            try {
                runnable.run();
            } catch (Throwable t) {
                LOGGER.error("Error while executing task", t);
            }
        }
        return hasWork;
    }

    private boolean handlePendingWrites() {
        boolean hasWork = false;
        WriteRequest writeRequest;
        while ((writeRequest = this.pendingWriteRequests.poll()) != null) {
            hasWork = true;
            this.cache.put(writeRequest.pos, writeRequest.nbt);
            this.writeBacklog.put(writeRequest.pos, writeRequest.nbt);
            this.writeCompletions.computeIfAbsent(writeRequest.pos, unused -> new ObjectArrayList<>(1)).add(writeRequest.future);
        }
        return hasWork;
    }

    private void completeWriteFutures(long pos, @Nullable Throwable throwable) {
        final ObjectArrayList<CompletableFuture<Void>> futures = this.writeCompletions.remove(pos);
        if (futures == null) return;
        for (CompletableFuture<Void> future : futures) {
            if (throwable != null) future.completeExceptionally(throwable);
            else future.complete(null);
        }
    }

    private boolean handlePendingReads() {
        boolean hasWork = false;
        ReadRequest readRequest;
        while ((readRequest = this.pendingReadRequests.poll()) != null) {
            hasWork = true;
            final long pos = readRequest.pos;
            final CompletableFuture<CompoundTag> future = readRequest.future;
            final StreamTagVisitor scanner = readRequest.scanner;
            try {
                final Either<CompoundTag, byte[]> cached = this.cache.getOrDefault(pos, CACHE_MISS);
                if (cached != CACHE_MISS) {
                    if (cached == null) {
                        future.complete(null);
                    } else if (cached.left().isPresent()) {
                        if (scanner != null) {
                            GlobalExecutors.executor.execute(() -> {
                                try {
                                    cached.left().get().acceptAsRoot(scanner);
                                    future.complete(null);
                                } catch (Throwable t) {
                                    future.completeExceptionally(t);
                                }
                            });
                        } else {
                            future.complete(cached.left().get());
                        }
                    } else {
                        CompletableFuture.supplyAsync(() -> {
                                    try {
                                        final DataInputStream input = new DataInputStream(new ByteArrayInputStream(cached.right().get()));
                                        if (scanner != null) {
                                            NbtIo.parse(input, scanner);
                                            return null;
                                        } else {
                                            final CompoundTag compound = NbtIo.read(input);
                                            return compound;
                                        }
                                    } catch (IOException e) {
                                        SneakyThrow.sneaky(e);
                                        return null; // unreachable
                                    }
                                }, GlobalExecutors.executor)
                                .thenAccept(future::complete)
                                .exceptionally(throwable -> {
                                    future.completeExceptionally(throwable);
                                    return null;
                                });
                    }
                    continue;
                }
                scheduleChunkRead(pos, future, scanner);
            } catch (Throwable t) {
                LOGGER.error("Error reading chunk {}", new ChunkPos(pos), t);
                future.completeExceptionally(t);
            }
        }
        return hasWork;
    }

    private boolean writeBacklog() {
        if (!this.writeBacklog.isEmpty()) {
            final long pos = this.writeBacklog.firstLongKey();
            final Either<CompoundTag, byte[]> nbt = this.writeBacklog.removeFirst();
            try {
                writeChunk(pos, nbt);
            } catch (Throwable t) {
                LOGGER.error("Error writing chunk {}", new ChunkPos(pos), t);
                completeWriteFutures(pos, t);
            }
            return true;
        }
        return false;
    }

    private void runWriteFutureGC() {
        this.writeFutures.removeIf(CompletableFuture::isDone);
    }

    private void flushBacklog() {
        while (!this.writeFutures.isEmpty()) {
            while (writeBacklog()) ;
            runWriteFutureGC();
            final CompletableFuture<?>[] pending = new CompletableFuture[this.writeFutures.size()];
            int index = 0;
            for (CompletableFuture<Void> future : this.writeFutures) {
                pending[index++] = future.exceptionally(unused -> null);
            }
            final CompletableFuture<Void> allFuture = CompletableFuture.allOf(pending);
            while (!allFuture.isDone()) {
                boolean hasWork = handleTasks();
                hasWork = handlePendingReads() || hasWork;
                if (!hasWork) LockSupport.parkNanos("Waiting for pending chunk writes", SPIN_PARK_NANOS);
            }
            runWriteFutureGC();
        }
    }

    private void scheduleChunkRead(long pos, CompletableFuture<CompoundTag> future, StreamTagVisitor scanner) {
        try {
            final ChunkPos pos1 = new ChunkPos(pos);
            final RegionFile regionFile = RegionFileUtil.getRegionFile(this.storage, pos1, false);
            final DataInputStream chunkInputStream = regionFile != null ? regionFile.getChunkDataInputStream(pos1) : null;
            if (chunkInputStream == null) {
                future.complete(null);
                return;
            }
            CompletableFuture.supplyAsync(() -> {
                try {
                    try (DataInputStream inputStream = chunkInputStream) {
                        if (scanner != null) {
                            NbtIo.parse(inputStream, scanner);
                            return null;
                        } else {
                            return NbtIo.read(inputStream);
                        }
                    }
                } catch (Throwable t) {
                    SneakyThrow.sneaky(t);
                    return null; // Unreachable anyway
                }
            }, GlobalExecutors.executor).handle((compound, throwable) -> {
                if (throwable != null) future.completeExceptionally(throwable);
                else future.complete(compound);
                return null;
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
    }

    private void writeChunk(long pos, Either<CompoundTag, byte[]> nbt) {
        if (nbt == null) {
            if (this.cache.get(pos) == null) {
                Throwable error = null;
                try {
                    final ChunkPos pos1 = new ChunkPos(pos);
                    final RegionFile regionFile = RegionFileUtil.getRegionFile(this.storage, pos1, false);
                    if (regionFile != null) regionFile.clear(pos1);
                } catch (Throwable t) {
                    LOGGER.error("Error deleting chunk {}", new ChunkPos(pos), t);
                    error = t;
                }
                this.cache.remove(pos);
                completeWriteFutures(pos, error);
            }
        } else {
            final CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                try {
                    final RawByteArrayOutputStream out = new RawByteArrayOutputStream(8096);
                    // TODO [VanillaCopy] RegionFile.ChunkBuffer
                    out.write(0);
                    out.write(0);
                    out.write(0);
                    out.write(0);
                    out.write(ConfigConstants.CHUNK_STREAM_VERSION.getId());
                    try (DataOutputStream dataOutputStream = new DataOutputStream(ConfigConstants.CHUNK_STREAM_VERSION.wrap(out))) {
                        if (nbt.left().isPresent()) {
                            NbtIo.write(nbt.left().get(), dataOutputStream);
                        } else {
                            dataOutputStream.write(nbt.right().get());
                        }
                    }
                    return out;
                } catch (Throwable t) {
                    SneakyThrow.sneaky(t);
                    return null; // Unreachable anyway
                }
            }, GlobalExecutors.executor).thenApplyAsync(bytes -> {
                if (nbt != this.cache.get(pos)) return Boolean.FALSE;
                try {
                    final ChunkPos pos1 = new ChunkPos(pos);
                    final RegionFile regionFile = RegionFileUtil.getRegionFile(this.storage, pos1, true);
                    ByteBuffer byteBuffer = bytes.asByteBuffer();
                    // TODO [VanillaCopy] RegionFile.ChunkBuffer
                    byteBuffer.putInt(0, bytes.size() - 5 + 1);
                    ((IRegionFile) regionFile).invokeWriteChunk(pos1, byteBuffer);
                } catch (Throwable t) {
                    SneakyThrow.sneaky(t);
                }
                this.cache.remove(pos);
                return Boolean.TRUE;
            }, this.executor).handleAsync((written, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Error writing chunk {}", new ChunkPos(pos), throwable);
                    completeWriteFutures(pos, throwable);
                } else if (written) {
                    completeWriteFutures(pos, null);
                }
                return (Void) null;
            }, this.executor);
            this.writeFutures.add(future);
        }
    }

    private record ReadRequest(long pos, CompletableFuture<CompoundTag> future, @Nullable StreamTagVisitor scanner) {
    }

    private record WriteRequest(long pos, @Nullable Either<CompoundTag, byte[]> nbt, CompletableFuture<Void> future) {
    }

}
