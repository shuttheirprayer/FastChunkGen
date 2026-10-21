package com.misanthropy.fastchunkgen.threading.chunkio.common;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;

public class Config {

    public static final boolean recoverFromErrors = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.recoverFromErrors")
            .comment("Regenerate chunks that fail to load. Genuienly regenerates them. Whatever was inside it is gone.")
            .getBoolean(true, true);

    public static final boolean fireForgeChunkDataEvents = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.fireForgeChunkDataEvents")
            .comment("Fire Forge ChunkDataEvent.Save on async saves. Turning off may cause some data-loss.")
            .getBoolean(true, true);

    public static final boolean forgeChunkDataEventsOnMainThread = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.forgeChunkDataEventsOnMainThread")
            .comment("Fire Forge chunk data events and deserialize chunk capabilities on the server thread. Crash and data-loss risk on some mods.")
            .getBoolean(true, true);

    public static final boolean serializeBlockEntitiesOnMainThread = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.serializeBlockEntitiesOnMainThread")
            .comment("Snapshot block entity NBT on the server thread when a chunk save starts. Disabling it WILL improve performance but will also cause many possible problems in modded environments.")
            .getBoolean(true, true);

}
