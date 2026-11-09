package com.misanthropy.fastchunkgen.rewrites.chunkio.mixin;

import com.misanthropy.fastchunkgen.rewrites.chunkio.common.C2MEStorageVanillaInterface;
import com.misanthropy.fastchunkgen.rewrites.chunkio.common.ChunkWriteCompletion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStorage.class)
public class MixinVersionedChunkStorage {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/world/level/chunk/storage/IOWorker"))
    private IOWorker redirectStorageIoWorker(Path directory, boolean dsync, String name) {
        return new C2MEStorageVanillaInterface(directory, dsync, name);
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/IOWorker;store(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> captureWriteCompletion(IOWorker worker, ChunkPos pos, CompoundTag nbt) {
        final CompletableFuture<Void> future = worker.store(pos, nbt);
        ChunkWriteCompletion.set(future);
        return future;
    }

}
