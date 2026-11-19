package com.misanthropy.fastchunkgen.threading.worldgen.common;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public record UnloadedChunk(int x, int z, Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> cause) implements ChunkHolder.ChunkLoadingFailure {
    @Override
    public String toString() {
        return cause == null ? "Unloaded " + new ChunkPos(x, z) : "Unloaded " + new ChunkPos(x, z) + " " + cause.right().get();
    }
}
