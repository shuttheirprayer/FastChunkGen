package com.misanthropy.fastchunkgen.rewrites.chunkio.mixin;

import com.misanthropy.fastchunkgen.rewrites.chunkio.common.C2MEStorageVanillaInterface;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Mixin(EntityStorage.class)
public class MixinEntityChunkDataAccess {

    @Shadow
    @Final
    private ServerLevel level;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/world/level/chunk/storage/IOWorker"))
    private IOWorker redirectStorageIoWorker(Path directory, boolean dsync, String name) {
        return new C2MEStorageVanillaInterface(directory, dsync, name);
    }

}
