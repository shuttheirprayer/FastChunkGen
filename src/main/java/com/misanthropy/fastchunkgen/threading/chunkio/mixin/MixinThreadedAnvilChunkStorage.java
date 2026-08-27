package com.misanthropy.fastchunkgen.threading.chunkio.mixin;

import com.ibm.asyncutil.locks.AsyncNamedLock;
import com.misanthropy.fastchunkgen.base.common.GlobalExecutors;
import com.misanthropy.fastchunkgen.base.common.registry.SerializerAccess;
import com.misanthropy.fastchunkgen.base.common.theinterface.IDirectStorage;
import com.misanthropy.fastchunkgen.base.common.util.SneakyThrow;
import com.misanthropy.fastchunkgen.base.mixin.access.IVersionedChunkStorage;
import com.misanthropy.fastchunkgen.rewrites.chunkio.common.ChunkWriteCompletion;
import com.misanthropy.fastchunkgen.threading.chunkio.common.AsyncSerializationManager;
import com.misanthropy.fastchunkgen.threading.chunkio.common.BlendingInfoUtil;
import com.misanthropy.fastchunkgen.threading.chunkio.common.ChunkIoMainThreadTaskUtils;
import com.misanthropy.fastchunkgen.threading.chunkio.common.Config;
import com.misanthropy.fastchunkgen.threading.chunkio.common.ForgeChunkDataEvents;
import com.misanthropy.fastchunkgen.threading.chunkio.common.IAsyncChunkStorage;
import com.misanthropy.fastchunkgen.threading.chunkio.common.ISerializingRegionBasedStorage;
import com.misanthropy.fastchunkgen.threading.chunkio.common.ProtoChunkExtension;
import com.misanthropy.fastchunkgen.threading.chunkio.common.TaskCancellationException;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage extends ChunkStorage implements ChunkHolder.PlayerProvider {

    public MixinThreadedAnvilChunkStorage(Path path, DataFixer dataFixer, boolean bl) {
        super(path, dataFixer, bl);
    }

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private PoiManager poiManager;

    @Shadow
    protected abstract byte markPosition(ChunkPos chunkPos, ChunkStatus.ChunkType chunkType);

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    protected abstract void markPositionReplaceable(ChunkPos chunkPos);

    @Shadow
    @Final
    private Supplier<DimensionDataStorage> overworldDataStorage;

    @Shadow
    @Final
    private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow
    protected abstract boolean isExistingChunkFull(ChunkPos chunkPos);

    @Shadow
    private ChunkGenerator generator;

    @Shadow
    protected abstract boolean save(ChunkAccess chunk);

    @Shadow
    protected abstract void saveAllChunks(boolean flush);

    @Shadow
    private static boolean isChunkDataValid(CompoundTag nbtCompound) {
        throw new AbstractMethodError();
    }

    @Shadow protected abstract ChunkAccess createEmptyChunk(ChunkPos chunkPos);

    @Mutable
    @Shadow @Final private Long2ByteMap chunkTypeCache;

    @Shadow protected abstract CompoundTag upgradeChunkTag(CompoundTag nbt);

    private AsyncNamedLock<ChunkPos> chunkLock = AsyncNamedLock.createFair();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        chunkLock = AsyncNamedLock.createFair();
        this.chunkTypeCache = Long2ByteMaps.synchronize(this.chunkTypeCache);
    }

    private Set<ChunkPos> scheduledChunks = new HashSet<>();

    /**
     * @author ishland
     * @reason async io and deserialization
     */
    @Overwrite
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos pos) {
        if (scheduledChunks == null) scheduledChunks = new HashSet<>();
        synchronized (scheduledChunks) {
            if (scheduledChunks.contains(pos)) throw new IllegalArgumentException("Already scheduled");
            scheduledChunks.add(pos);
        }

        final CompletableFuture<Optional<CompoundTag>> poiData =
                ((IAsyncChunkStorage) ((com.misanthropy.fastchunkgen.base.mixin.access.ISerializingRegionBasedStorage) this.poiManager).getWorker()).getNbtAtAsync(pos)
                        .exceptionally(throwable -> {
                            //noinspection IfStatementWithIdenticalBranches
                            if (Config.recoverFromErrors) {
                                LOGGER.error("Couldn't load poi data for chunk {}, poi data will be lost!", pos, throwable);
                                return Optional.empty();
                            } else {
                                SneakyThrow.sneaky(throwable);
                                return Optional.empty(); // unreachable
                            }
                        });

        final ReferenceArrayList<Runnable> mainThreadQueue = new ReferenceArrayList<>();

        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = getUpdatedChunkNbtAtAsync(pos)
                .thenApply(optional -> optional.filter(nbtCompound -> {
                    boolean bl = isChunkDataValid(nbtCompound);
                    if (!bl) {
                        LOGGER.error("Chunk file at {} is missing level data, skipping", pos);
                    }

                    return bl;
                }))
                .thenApplyAsync(optional -> {
                    if (optional.isPresent()) {
                        ChunkIoMainThreadTaskUtils.push(mainThreadQueue);
                        try {
                            return ChunkSerializer.read(this.level, this.poiManager, pos, optional.get());
                        } finally {
                            ChunkIoMainThreadTaskUtils.pop(mainThreadQueue);
                        }
                    }

                    return null;
                }, GlobalExecutors.executor)
                .exceptionally(throwable -> {
                    //noinspection IfStatementWithIdenticalBranches
                    if (Config.recoverFromErrors) {
                        LOGGER.error("Couldn't load chunk {}, chunk data will be lost!", pos, throwable);
                        return null;
                    } else {
                        SneakyThrow.sneaky(throwable);
                        return null; // unreachable
                    }
                })
//                .thenCombine(poiData, (protoChunk, tag) -> protoChunk)
//                .thenCombine(blendingInfos, (protoChunk, bitSet) -> {
//                    if (protoChunk != null) ((ProtoChunkExtension) protoChunk).setBlendingInfo(pos, bitSet);
//                    return protoChunk;
//                })
                .thenApplyAsync(protoChunk -> {
                    // blending
                    protoChunk = protoChunk != null ? protoChunk : (ProtoChunk) this.createEmptyChunk(pos);
                    if (protoChunk.getBelowZeroRetrogen() != null || protoChunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
                        final CompletionStage<List<BitSet>> blendingInfos = BlendingInfoUtil.getBlendingInfos((IOWorker) this.chunkScanner(), pos);
                        ProtoChunk finalProtoChunk = protoChunk;
                        ((ProtoChunkExtension) protoChunk).setBlendingComputeFuture(
                                blendingInfos.thenAccept(bitSet -> ((ProtoChunkExtension) finalProtoChunk).setBlendingInfo(pos, bitSet)).toCompletableFuture()
                        );
                    }

                    ((ProtoChunkExtension) protoChunk).setInitialMainThreadComputeFuture(poiData.thenAcceptAsync(poiDataNbt -> {
                        try {
                            ((ISerializingRegionBasedStorage) this.poiManager).update(pos, poiDataNbt.orElse(null));
                        } catch (Throwable t) {
                            if (Config.recoverFromErrors) {
                                LOGGER.error("Couldn't load poi data for chunk {}, poi data will be lost!", pos, t);
                            } else {
                                SneakyThrow.sneaky(t);
                            }
                        }
                        ChunkIoMainThreadTaskUtils.drainQueue(mainThreadQueue);
                    }, this.mainThreadExecutor));

                    this.markPosition(pos, protoChunk.getStatus().getChunkType());
                    return Either.left(protoChunk);
                }, GlobalExecutors.invokingExecutor);
        future.exceptionally(throwable -> {
            LOGGER.error("Couldn't load chunk {}", pos, throwable);
            return null;
        });
        future.exceptionally(throwable -> null).thenRun(() -> {
            synchronized (scheduledChunks) {
                scheduledChunks.remove(pos);
            }
        });
        return future;

        // [VanillaCopy] - for reference
        /*
        return CompletableFuture.supplyAsync(() -> {
         try {
            this.world.getProfiler().visit("chunkLoad");
            CompoundTag compoundTag = this.getUpdatedChunkNbt(pos);
            if (compoundTag != null) {
               boolean bl = compoundTag.contains("Level", 10) && compoundTag.getCompound("Level").contains("Status", 8);
               if (bl) {
                  Chunk chunk = ChunkSerializer.deserialize(this.world, this.structureManager, this.pointOfInterestStorage, pos, compoundTag);
                  this.method_27053(pos, chunk.getStatus().getChunkType());
                  return Either.left(chunk);
               }

               LOGGER.error((String)"Chunk file at {} is missing level data, skipping", (Object)pos);
            }
         } catch (CrashException var5) {
            Throwable throwable = var5.getCause();
            if (!(throwable instanceof IOException)) {
               this.method_27054(pos);
               throw var5;
            }

            LOGGER.error((String)"Couldn't load chunk {}", (Object)pos, (Object)throwable);
         } catch (Exception var6) {
            LOGGER.error((String)"Couldn't load chunk {}", (Object)pos, (Object)var6);
         }

         this.method_27054(pos);
         return Either.left(new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, this.world));
      }, this.mainThreadExecutor);
         */
    }

    private CompletableFuture<Optional<CompoundTag>> getUpdatedChunkNbtAtAsync(ChunkPos pos) {
        return readChunk(pos);
    }

    /**
     * @author ishland
     * @reason skip datafixer if possible
     */
    @Overwrite
    public CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos chunkPos) {
//        return this.getNbt(chunkPos).thenApplyAsync(nbt -> nbt.map(this::updateChunkNbt), Util.getMainWorkerExecutor());
        return this.read(chunkPos).thenCompose(nbt -> {
            if (nbt.isPresent()) {
                final CompoundTag compound = nbt.get();
                if (ChunkStorage.getVersion(compound) != SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
                    return CompletableFuture.supplyAsync(() -> Optional.of(upgradeChunkTag(compound)), Util.backgroundExecutor());
                } else {
                    return CompletableFuture.completedFuture(nbt);
                }
            } else {
                return CompletableFuture.completedFuture(Optional.empty());
            }
        });
    }

    @ModifyReturnValue(method = "schedule", at = @At("RETURN"))
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> postGetChunk(CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> originalReturn, ChunkHolder holder, ChunkStatus requiredStatus) {
        if (requiredStatus == ChunkStatus.FULL.getParent()) {
            // wait for initial main thread tasks before proceeding to finish full chunk
            return originalReturn.thenCompose(either -> {
                if (either.left().isPresent()) {
                    final ChunkAccess chunk = either.left().get();
                    if (chunk instanceof ProtoChunk protoChunk) {
                        final CompletableFuture<Void> future = ((ProtoChunkExtension) protoChunk).getInitialMainThreadComputeFuture();
                        if (future != null) {
                            return future.thenApply(v -> either);
                        }
                    }
                }
                return CompletableFuture.completedFuture(either);
            });
        }
        return originalReturn;
    }

    private ConcurrentLinkedQueue<CompletableFuture<Void>> saveFutures = new ConcurrentLinkedQueue<>();

    @Dynamic
    @Redirect(method = "m_202998_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z"))
    // method: consumer in tryUnloadChunk
    private boolean asyncSave(ChunkMap tacs, ChunkAccess chunk, ChunkHolder holder) {
        // TODO [VanillaCopy] - check when updating minecraft version
        this.poiManager.flush(chunk.getPos());
        if (!chunk.isUnsaved()) {
            return false;
        } else {
            chunk.setUnsaved(false);
            ChunkPos chunkPos = chunk.getPos();

            try {
                ChunkStatus chunkStatus = chunk.getStatus();
                if (chunkStatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    if (this.isExistingChunkFull(chunkPos)) {
                        return false;
                    }

                    if (chunkStatus == ChunkStatus.EMPTY) {
                        boolean hasValidStart = false;
                        for (StructureStart start : chunk.getAllStarts().values()) {
                            if (start.isValid()) {
                                hasValidStart = true;
                                break;
                            }
                        }
                        if (!hasValidStart) return false;
                    }
                }

                final CompletableFuture<ChunkAccess> originalSavingFuture = holder.getChunkToSave();
                if (!originalSavingFuture.isDone()) {
                    originalSavingFuture.handleAsync((_unused, __unused) -> asyncSave(tacs, chunk, holder), this.mainThreadExecutor);
                    return false;
                }

                this.level.getProfiler().incrementCounter("chunkSave");
                // C2ME start - async serialization
                if (saveFutures == null) saveFutures = new ConcurrentLinkedQueue<>();
                AsyncSerializationManager.Scope scope = new AsyncSerializationManager.Scope(chunk, level);

                saveFutures.add(chunkLock.acquireLock(chunk.getPos()).toCompletableFuture().thenCompose(lockToken ->
                        CompletableFuture.supplyAsync(() -> {
                                    scope.open();
                                    if (holder.getChunkToSave() != originalSavingFuture) {
                                        this.mainThreadExecutor.execute(() -> asyncSave(tacs, chunk, holder));
                                        throw new TaskCancellationException();
                                    }
                                    AsyncSerializationManager.push(scope);
                                    try {
                                        return SerializerAccess.getSerializer().serialize(level, chunk);
                                    } finally {
                                        AsyncSerializationManager.pop(scope);
                                    }
                                }, GlobalExecutors.executor)
                                .thenCompose((either) -> {
                                    if (either.left().isPresent()) {
                                        final CompoundTag serialized = either.left().get();
                                        return ForgeChunkDataEvents.fireSave(this.level, chunk, serialized, this.mainThreadExecutor)
                                                .thenCompose(unused -> {
                                                    this.write(chunkPos, serialized);
                                                    return ChunkWriteCompletion.take();
                                                });
                                    } else {
                                        return ((IDirectStorage) ((IVersionedChunkStorage) this).getWorker()).setRawChunkData(chunkPos, either.right().get());
                                    }
                                })
                                .handle((unused, throwable) -> {
                                    lockToken.releaseLock();
                                    if (throwable != null) {
                                        Throwable actual = throwable;
                                        while (actual instanceof CompletionException e) actual = e.getCause();
                                        if (!(actual instanceof TaskCancellationException)) {
                                            LOGGER.error("Failed to save chunk {},{} asynchronously, falling back to sync saving", chunkPos.x, chunkPos.z, throwable);
                                            chunk.setUnsaved(true);
                                            final CompletableFuture<ChunkAccess> savingFuture = holder.getChunkToSave();
                                            if (savingFuture != originalSavingFuture) {
                                                savingFuture.handleAsync((_unused, __unused) -> save(chunk), this.mainThreadExecutor);
                                            } else {
                                                this.mainThreadExecutor.execute(() -> this.save(chunk));
                                            }
                                        }
                                    }
                                    return unused;
                                })
                ));
                this.markPosition(chunkPos, chunkStatus.getChunkType());
                // C2ME end
                return true;
            } catch (Exception var5) {
                LOGGER.error((String) "Failed to save chunk {},{}", (Object) chunkPos.x, chunkPos.z, var5);
                chunk.setUnsaved(true);
                return false;
            }
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        GlobalExecutors.executor.execute(() -> saveFutures.removeIf(CompletableFuture::isDone));
    }

    @Override
    public void flushWorker() {
        final CompletableFuture<Void> future = CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]));
        this.mainThreadExecutor.managedBlock(future::isDone); // wait for serialization to complete
        super.flushWorker();
    }
}
