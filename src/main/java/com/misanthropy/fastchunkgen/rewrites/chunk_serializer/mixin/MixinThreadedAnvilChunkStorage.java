package com.misanthropy.fastchunkgen.rewrites.chunk_serializer.mixin;

import com.misanthropy.fastchunkgen.base.common.theinterface.IDirectStorage;
import com.misanthropy.fastchunkgen.base.mixin.access.IVersionedChunkStorage;
import com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common.ChunkDataSerializer;
import com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common.NbtWriter;
import com.mojang.datafixers.DataFixer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.structure.StructureStart;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage extends ChunkStorage {
    @Final
    @Shadow
    private static Logger LOGGER;

    @Final
    @Shadow
    private PoiManager poiManager;

    @Final
    @Shadow
    ServerLevel level;

    @Shadow
    private native boolean isExistingChunkFull(ChunkPos chunkPos);

    @Shadow
    private native byte markPosition(ChunkPos chunkPos, ChunkStatus.ChunkType chunkType);

    public MixinThreadedAnvilChunkStorage(Path directory, DataFixer dataFixer, boolean dsync) {
        super(directory, dataFixer, dsync);
    }

    /**
     * @author Kroppeb
     * @reason Reduces allocations
     */
    @Overwrite()
    private boolean save(ChunkAccess chunk) {
        // [VanillaCopy]
        this.poiManager.flush(chunk.getPos());
        if (!chunk.isUnsaved()) {
            return false;
        }

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

            this.level.getProfiler().incrementCounter("chunkSave");

            //region start replaced code
            // NbtCompound nbtCompound = ChunkSerializer.serialize(this.world, chunk);
            NbtWriter nbtWriter = new NbtWriter();
            try {
                nbtWriter.start(Tag.TAG_COMPOUND);
                ChunkDataSerializer.write(this.level, chunk, nbtWriter);
                nbtWriter.finishCompound();

            // this.setNbt(chunkPos, nbtCompound);
            // temp fix, idk,
//            var storageWorker = (StorageIoWorkerAccessor) this.getIoWorker();
//            var storage = (RegionBasedStorageAccessor) (Object) storageWorker.getStorage();
//            storageWorker.invokeRun(() -> {
//                try {
//                    DataOutputStream chunkOutputStream = storage.invokeGetRegionFile(chunkPos).getChunkOutputStream(chunkPos);
//                    chunkOutputStream.write(nbtWriter.toByteArray());
//                    chunkOutputStream.close();
//                    nbtWriter.release();
//                    return Either.left((Void) null);
//                } catch (Exception t) {
//                    return Either.right(t);
//                }
//            });
                ((IDirectStorage) ((IVersionedChunkStorage) this).getWorker()).setRawChunkData(chunkPos, nbtWriter.toByteArray());
            } finally {
                nbtWriter.release();
            }

            //endregion end replaced code

            this.markPosition(chunkPos, chunkStatus.getChunkType());
            return true;
        } catch (Exception var5) {
            LOGGER.error("Failed to save chunk {},{}", chunkPos.x, chunkPos.z, var5);
            return false;
        }
    }
}
