package com.misanthropy.fastchunkgen.threading.chunkio.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IAsyncChunkStorage {

    CompletableFuture<Optional<CompoundTag>> getNbtAtAsync(ChunkPos pos);

}
