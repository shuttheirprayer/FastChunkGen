package com.misanthropy.fastchunkgen.rewrites.chunk_serializer;

import com.ibm.asyncutil.util.Either;
import com.misanthropy.fastchunkgen.base.common.registry.SerializerAccess;
import com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common.ChunkDataSerializer;
import com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common.NbtWriter;
import net.minecraft.nbt.Tag;

public class TheMod {

    public static void init() {
        if (ModuleEntryPoint.enabled) {
            SerializerAccess.registerSerializer((world, chunk) -> {
                NbtWriter nbtWriter = new NbtWriter();
                try {
                    nbtWriter.start(Tag.TAG_COMPOUND);
                    ChunkDataSerializer.write(world, chunk, nbtWriter);
                    nbtWriter.finishCompound();
                    return Either.right(nbtWriter.toByteArray());
                } finally {
                    nbtWriter.release();
                }
            });
        }
    }
}
