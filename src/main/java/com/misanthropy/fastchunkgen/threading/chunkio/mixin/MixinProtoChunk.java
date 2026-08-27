package com.misanthropy.fastchunkgen.threading.chunkio.mixin;

import com.misanthropy.fastchunkgen.base.mixin.access.IBlender;
import com.misanthropy.fastchunkgen.threading.chunkio.common.ProtoChunkExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;

@Mixin(ProtoChunk.class)
public class MixinProtoChunk implements ProtoChunkExtension {

    @Unique
    private CompletableFuture<Void> blendingComputeFuture = CompletableFuture.completedFuture(null);

    @Unique
    private CompletableFuture<Void> initialMainThreadComputeFuture = CompletableFuture.completedFuture(null);

    @Unique
    private boolean needBlending = false;

    @Override
    public void setBlendingInfo(ChunkPos pos, List<BitSet> bitSets) {
        final int radius = IBlender.getBLENDING_CHUNK_DISTANCE_THRESHOLD();
        final int minX = pos.x - radius;
        final int minZ = pos.z - radius;
        final int maxX = pos.x + radius;
        final int maxZ = pos.z + radius;

        int index = 0;
        for(int i = minX >> 5; i <= maxX >> 5; ++i) {
            for(int j = minZ >> 5; j <= maxZ >> 5; ++j) {
                BitSet bitSet = bitSets.get(index ++);
                if (!bitSet.isEmpty()) {
                    final int regionMinX = i << 5;
                    final int regionMinZ = j << 5;
                    int k = Math.max(minX - regionMinX, 0);
                    int l = Math.max(minZ - regionMinZ, 0);
                    int m = Math.min(maxX - regionMinX, 31);
                    int n = Math.min(maxZ - regionMinZ, 31);

                    for(int o = k; o <= m; ++o) {
                        for(int p = l; p <= n; ++p) {
                            int q = p * 32 + o;
                            if (bitSet.get(q)) {
                                this.needBlending = true;
                                return;
                            }
                        }
                    }
                }
            }
        }

        this.needBlending = false;
    }

    @Override
    public void setBlendingComputeFuture(CompletableFuture<Void> future) {
        this.blendingComputeFuture = future;
    }

    @Override
    public boolean getNeedBlending() {
        if (!blendingComputeFuture.isDone()) {
            blendingComputeFuture.join();
        }
        return needBlending;
    }

    @Override
    public void setInitialMainThreadComputeFuture(CompletableFuture<Void> future) {
        this.initialMainThreadComputeFuture = future;
    }

    @Override
    public CompletableFuture<Void> getInitialMainThreadComputeFuture() {
        return this.initialMainThreadComputeFuture;
    }

}
