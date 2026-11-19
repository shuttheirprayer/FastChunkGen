package com.misanthropy.fastchunkgen.threading.worldgen.mixin;

import com.misanthropy.fastchunkgen.threading.worldgen.common.UnloadedChunk;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

@Mixin(ChunkMap.class)
public abstract class MixinChunkRangeFuture {

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    @Shadow protected abstract ReportedException debugFuturesAndCreateReportedException(IllegalStateException exception, String details);

    /**
     * @author Misanthropy
     * @reason same futures as vanilla without building a save dependency label string per neighbor
     */
    @Overwrite
    private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkHolder holder, int margin, IntFunction<ChunkStatus> distanceToStatus) {
        if (margin == 0) {
            ChunkStatus status = distanceToStatus.apply(0);
            return holder.getOrScheduleFuture(status, (ChunkMap) (Object) this).thenApply(either -> either.mapLeft(List::of));
        }
        int side = margin * 2 + 1;
        List<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures = new ArrayList<>(side * side);
        List<ChunkHolder> holders = new ArrayList<>(side * side);
        ChunkPos center = holder.getPos();
        int cx = center.x;
        int cz = center.z;
        for (int dz = -margin; dz <= margin; dz++) {
            for (int dx = -margin; dx <= margin; dx++) {
                int distance = Math.max(Math.abs(dx), Math.abs(dz));
                ChunkHolder neighbour = this.visibleChunkMap.get(ChunkPos.asLong(cx + dx, cz + dz));
                if (neighbour == null) {
                    return CompletableFuture.completedFuture(Either.right(new UnloadedChunk(cx + dx, cz + dz, null)));
                }
                futures.add(neighbour.getOrScheduleFuture(distanceToStatus.apply(distance), (ChunkMap) (Object) this));
                holders.add(neighbour);
            }
        }
        CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> range = Util.sequence(futures).thenApply(results -> {
            List<ChunkAccess> chunks = new ArrayList<>(results.size());
            int k = 0;
            for (Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either : results) {
                if (either == null) {
                    throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null!"), "n/a");
                }
                ChunkAccess chunk = either.left().orElse(null);
                if (chunk == null) {
                    return Either.right(new UnloadedChunk(cx + k % side - margin, cz + k / side - margin, either));
                }
                chunks.add(chunk);
                k++;
            }
            return Either.left(chunks);
        });
        for (ChunkHolder neighbour : holders) {
            ((ChunkHolderSaveDependency) neighbour).fcg$addSaveDependency("getChunkRangeFuture", range);
        }
        return range;
    }
}
