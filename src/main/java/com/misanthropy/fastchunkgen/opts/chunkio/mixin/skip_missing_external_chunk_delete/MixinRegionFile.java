package com.misanthropy.fastchunkgen.opts.chunkio.mixin.skip_missing_external_chunk_delete;

import net.minecraft.world.level.chunk.storage.RegionFile;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(RegionFile.class)
public class MixinRegionFile {

    @Dynamic
    @Redirect(method = "m_63690_", at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;deleteIfExists(Ljava/nio/file/Path;)Z"), require = 1, allow = 1)
    private boolean fastchunkgen$deleteExternalAfterWrite(Path path) throws IOException {
        return path.toFile().exists() && Files.deleteIfExists(path);
    }

    @Redirect(method = "clear", at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;deleteIfExists(Ljava/nio/file/Path;)Z"), require = 1, allow = 1)
    private boolean fastchunkgen$deleteExternalOnClear(Path path) throws IOException {
        return path.toFile().exists() && Files.deleteIfExists(path);
    }

}
