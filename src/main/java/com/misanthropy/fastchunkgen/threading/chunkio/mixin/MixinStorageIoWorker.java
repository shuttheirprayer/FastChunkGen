package com.misanthropy.fastchunkgen.threading.chunkio.mixin;

import com.misanthropy.fastchunkgen.threading.chunkio.common.ChunkIoThreadingExecutorUtils;
import com.misanthropy.fastchunkgen.threading.chunkio.common.IAsyncChunkStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

@Mixin(IOWorker.class)
public abstract class MixinStorageIoWorker implements IAsyncChunkStorage {

    @Shadow public abstract CompletableFuture<Optional<CompoundTag>> loadAsync(ChunkPos pos);

    @Shadow protected abstract boolean isOldChunk(CompoundTag nbt);

    @Shadow public abstract CompletableFuture<Void> scanChunk(ChunkPos pos, StreamTagVisitor scanner);

    @Shadow @Final private static Logger LOGGER;
    private ExecutorService threadExecutor;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;ioPool()Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService redirectIoWorkerExecutor() {
        return threadExecutor = Executors.newSingleThreadExecutor(ChunkIoThreadingExecutorUtils.ioWorkerFactory);
    }

    @Override
    public CompletableFuture<Optional<CompoundTag>> getNbtAtAsync(ChunkPos pos) {
        return loadAsync(pos);
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ProcessorMailbox;close()V", shift = At.Shift.AFTER))
    private void onClose(CallbackInfo ci) {
        threadExecutor.shutdown();
        while (!threadExecutor.isTerminated()) {
            LockSupport.parkNanos("Waiting for thread executor termination", 100_000);
        }
    }

    /**
     * @author ishland
     * @reason use async instead of flooding worker thread
     */
    @Overwrite
    private CompletableFuture<BitSet> createOldDataForRegion(int chunkX, int chunkZ) {
        ChunkPos chunkPos = ChunkPos.minFromRegion(chunkX, chunkZ);
        ChunkPos chunkPos2 = ChunkPos.maxFromRegion(chunkX, chunkZ);
        BitSet bitSet = new BitSet();
        final CompletableFuture[] futures = ChunkPos.rangeClosed(chunkPos, chunkPos2)
                .map(chunkPosx -> {
                    CollectFields selectiveNbtCollector = new CollectFields(new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data"));

                    return this.scanChunk(chunkPosx, selectiveNbtCollector)
                            .thenRun(() -> {
                                Tag nbtElement = selectiveNbtCollector.getResult();
                                if (nbtElement instanceof CompoundTag nbtCompound) {
                                    if (this.isOldChunk(nbtCompound)) {
                                        int i = chunkPosx.getRegionLocalZ() * 32 + chunkPosx.getRegionLocalX();
                                        synchronized (bitSet) {
                                            bitSet.set(i);
                                        }
                                    }
                                }
                            })
                            .exceptionally(throwable -> {
                                LOGGER.warn("Failed to scan chunk {}", chunkPosx, throwable);
                                return null;
                            });
                }).toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .thenApply(unused -> bitSet);
    }
}
