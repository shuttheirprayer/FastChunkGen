package com.misanthropy.fastchunkgen.threading.chunkio.common;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;

public class Config {

    public static final boolean recoverFromErrors = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.recoverFromErrors")
            .comment("Regenerate chunks that fail to load. Destroys their contents.")
            .getBoolean(false, false);

    public static final boolean fireForgeChunkDataEvents = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.fireForgeChunkDataEvents")
            .comment("Fire Forge ChunkDataEvent.Save on async saves. Off means mods lose per-chunk data.")
            .getBoolean(true, true);

    public static final boolean forgeChunkDataEventsOnMainThread = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.forgeChunkDataEventsOnMainThread")
            .comment("Fire Forge chunk data events and deserialize chunk capabilities on the server thread. Off is unsafe for most mods.")
            .getBoolean(true, true);

    public static final boolean serializeBlockEntitiesOnMainThread = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.serializeBlockEntitiesOnMainThread")
            .comment("Snapshot block entity NBT on the server thread when a chunk save starts. Disabling serializes them on IO workers instead, which is faster but races modded block entities and Forge capabilities.")
            .getBoolean(true, true);

}
