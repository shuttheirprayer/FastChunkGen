package com.misanthropy.fastchunkgen.threading.chunkio.common;

import com.ibm.asyncutil.util.Combinators;
import com.misanthropy.fastchunkgen.base.mixin.access.IBlender;
import com.misanthropy.fastchunkgen.base.mixin.access.IStorageIoWorker;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;

public class BlendingInfoUtil {

    public static CompletionStage<List<BitSet>> getBlendingInfos(IOWorker worker, ChunkPos pos) {
        final int radius = IBlender.getBLENDING_CHUNK_DISTANCE_THRESHOLD();
        final int minRegionX = (pos.x - radius) >> 5;
        final int minRegionZ = (pos.z - radius) >> 5;
        final int maxRegionX = (pos.x + radius) >> 5;
        final int maxRegionZ = (pos.z + radius) >> 5;
        List<CompletableFuture<BitSet>> futures = new ArrayList<>((maxRegionX - minRegionX + 1) * (maxRegionZ - minRegionZ + 1));
        for(int i = minRegionX; i <= maxRegionX; ++i) {
            for(int j = minRegionZ; j <= maxRegionZ; ++j) {
                futures.add(((IStorageIoWorker) worker).invokeGetOrComputeBlendingStatus(i, j));
            }
        }
        return Combinators.collect(futures, Collectors.toList());
    }

}
