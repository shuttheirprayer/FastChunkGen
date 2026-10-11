package com.misanthropy.fastchunkgen.opts.chunkio.common;

import com.misanthropy.fastchunkgen.base.common.config.ConfigSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

    static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Opts/ChunkIO Config");

    public static final long chunkDataCacheSoftLimit = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.chunkDataCacheSoftLimit")
            .comment("Cached chunk writes before flushing to disk starts.")
            .getLong(8192, 8192, ConfigSystem.LongChecks.POSITIVE_VALUES_ONLY);

    public static final long chunkDataCacheLimit = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.chunkDataCacheLimit")
            .comment("Hard cap on cached chunk writes.")
            .getLong(32768, 32768, ConfigSystem.LongChecks.POSITIVE_VALUES_ONLY);

    public static final long chunkStreamVersion = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.chunkStreamVersion")
            .comment("Compression for newly saved chunks. -1 vanilla, 1 GZip, 2 Zlib, 3 none. Existing chunks stay readable.")
            .getLong(-1, -1);

    public static final boolean skipMissingExternalChunkDelete = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.skipMissingExternalChunkDelete")
            .comment("Check that the oversized chunk file exists before deleting it after each chunk save. Avoids an internal exception per save on Windows.")
            .getBoolean(true, false);

    public static void init() {
    }

}
