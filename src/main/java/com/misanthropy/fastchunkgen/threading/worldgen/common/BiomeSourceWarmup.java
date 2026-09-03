package com.misanthropy.fastchunkgen.threading.worldgen.common;

import com.misanthropy.fastchunkgen.base.common.util.Log;
import com.misanthropy.fastchunkgen.base.mixin.access.IMultiNoiseBiomeSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraftforge.event.level.LevelEvent;

public class BiomeSourceWarmup {

    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        try {
            if (level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource biomeSource) {
                ((IMultiNoiseBiomeSource) biomeSource).invokeParameters();
            }
        } catch (Throwable t) {
            Log.THREADED_WORLDGEN.debug("Could not resolve biome parameters for {} ahead of world generation",
                    level.dimension().location(), t);
        }
    }

}
