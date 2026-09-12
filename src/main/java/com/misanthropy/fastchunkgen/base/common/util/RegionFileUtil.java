package com.misanthropy.fastchunkgen.base.common.util;

import com.misanthropy.fastchunkgen.base.mixin.access.IRegionBasedStorage;
import net.minecraft.FileUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RegionFileUtil {

    @Nullable
    public static RegionFile getRegionFile(RegionFileStorage storage, ChunkPos pos, boolean create) throws IOException {
        final IRegionBasedStorage access = (IRegionBasedStorage) (Object) storage;
        final RegionFile regionFile = access.invokeGetRegionFile(pos);
        if (regionFile != null || !create) return regionFile;
        final Path folder = access.getFolder();
        FileUtil.createDirectoriesSafe(folder);
        final Path path = folder.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + RegionFileStorage.ANVIL_EXTENSION);
        if (Files.notExists(path)) Files.createFile(path);
        final RegionFile created = access.invokeGetRegionFile(pos);
        if (created == null) throw new IOException("Region file " + path + " cannot be opened");
        return created;
    }

}
