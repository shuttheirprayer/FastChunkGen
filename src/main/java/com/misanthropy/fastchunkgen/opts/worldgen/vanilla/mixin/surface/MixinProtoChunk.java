package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.SurfaceWrites;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.EnumSet;

@Mixin(ProtoChunk.class)
public abstract class MixinProtoChunk implements SurfaceWrites.FastChunk {

    @Unique private boolean fcg$heightsCached;
    @Unique private int fcg$minY;
    @Unique private int fcg$maxY;
    @Unique private int fcg$minSection;
    @Unique private ChunkStatus fcg$heightmapStatus;
    @Unique private Heightmap[] fcg$heightmaps;

    @Unique
    private void fcg$cacheHeights() {
        final ProtoChunk self = (ProtoChunk) (Object) this;
        this.fcg$minY = self.getMinBuildHeight();
        this.fcg$maxY = self.getMaxBuildHeight();
        this.fcg$minSection = self.getMinSection();
        this.fcg$heightsCached = true;
    }

    @Override
    public int fcg$minY() {
        if (!this.fcg$heightsCached) this.fcg$cacheHeights();
        return this.fcg$minY;
    }

    @Override
    public int fcg$maxY() {
        if (!this.fcg$heightsCached) this.fcg$cacheHeights();
        return this.fcg$maxY;
    }

    @Override
    public int fcg$minSection() {
        if (!this.fcg$heightsCached) this.fcg$cacheHeights();
        return this.fcg$minSection;
    }

    @Override
    public Heightmap[] fcg$heightmapsAfter(ChunkStatus status) {
        if (this.fcg$heightmapStatus == status) return this.fcg$heightmaps;
        final ProtoChunk self = (ProtoChunk) (Object) this;
        final EnumSet<Heightmap.Types> types = status.heightmapsAfter();
        final Heightmap[] heightmaps = new Heightmap[types.size()];
        int i = 0;
        for (Heightmap.Types type : types) {
            if (!self.hasPrimedHeightmap(type)) return null;
            heightmaps[i++] = self.getOrCreateHeightmapUnprimed(type);
        }
        this.fcg$heightmaps = heightmaps;
        this.fcg$heightmapStatus = status;
        return heightmaps;
    }

}
