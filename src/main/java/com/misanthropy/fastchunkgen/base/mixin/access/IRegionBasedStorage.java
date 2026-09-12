package com.misanthropy.fastchunkgen.base.mixin.access;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(RegionFileStorage.class)
public interface IRegionBasedStorage {

    @Invoker
    RegionFile invokeGetRegionFile(ChunkPos pos) throws IOException;

    @Accessor
    Path getFolder();

}
