package com.misanthropy.fastchunkgen.rewrites.chunkio.common;

import com.google.common.base.Preconditions;
import com.misanthropy.fastchunkgen.base.common.theinterface.IDirectStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class C2MEStorageVanillaInterface extends IOWorker implements IDirectStorage {

    private final C2MEStorageThread backend;

    public C2MEStorageVanillaInterface(Path directory, boolean dsync, String name) {
        super(null, dsync, name);
        this.backend = new C2MEStorageThread(directory, dsync, name);
    }

    @Override
    public CompletableFuture<Void> store(ChunkPos pos, @Nullable CompoundTag nbt) {
        return this.backend.setChunkData(pos.toLong(), nbt);
    }

    @Override
    public CompletableFuture<Optional<CompoundTag>> loadAsync(ChunkPos pos) {
        return this.backend.getChunkData(pos.toLong(), null).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Void> synchronize(boolean sync) {
        return this.backend.flush(sync);
    }

    @Override
    public CompletableFuture<Void> scanChunk(ChunkPos pos, StreamTagVisitor scanner) {
        Preconditions.checkNotNull(scanner, "scanner");
        return this.backend.getChunkData(pos.toLong(), scanner).thenApply(unused -> null);
    }

    @Override
    public void close() {
        this.backend.close().join();
    }

    @Override
    public boolean isOldChunkAround(ChunkPos chunkPos, int i) {
        return super.isOldChunkAround(chunkPos, i);
    }


    @Override
    public CompletableFuture<Void> setRawChunkData(ChunkPos pos, byte[] data) {
        return this.backend.setChunkData(pos.toLong(), data);
    }
}
