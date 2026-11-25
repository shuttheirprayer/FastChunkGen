package com.misanthropy.fastchunkgen.opts.allocs.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Function;

public final class OreSectionAccess implements Function<BlockPos, BlockState>, AutoCloseable {
    private static final ThreadLocal<OreSectionAccess> LOCAL = ThreadLocal.withInitial(OreSectionAccess::new);

    private LevelAccessor level;
    private long[] keys = new long[16];
    private LevelChunkSection[] sections = new LevelChunkSection[16];
    private int count;
    @Nullable
    private LevelChunkSection last;
    private long lastKey;
    private double[] blobs = new double[64];
    private double[] depths = new double[32];
    private boolean open;

    private OreSectionAccess() {
    }

    public static OreSectionAccess open(LevelAccessor level) {
        OreSectionAccess access = LOCAL.get();
        if (access.open) {
            access = new OreSectionAccess();
        }
        access.level = level;
        access.open = true;
        return access;
    }

    public double[] blobs(int length) {
        if (this.blobs.length < length) {
            this.blobs = new double[length];
        }
        return this.blobs;
    }

    public double[] depths(int length) {
        if (this.depths.length < length) {
            this.depths = new double[length * 2];
        }
        return this.depths;
    }

    @Nullable
    public LevelChunkSection getSection(BlockPos pos) {
        int index = this.level.getSectionIndex(pos.getY());
        if (index < 0 || index >= this.level.getSectionsCount()) {
            return null;
        }
        long key = SectionPos.asLong(pos);
        if (this.last == null || this.lastKey != key) {
            LevelChunkSection found = null;
            for (int i = 0; i < this.count; i++) {
                if (this.keys[i] == key) {
                    found = this.sections[i];
                    break;
                }
            }
            if (found == null) {
                ChunkAccess chunk = this.level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
                found = chunk.getSection(index);
                found.acquire();
                if (this.count == this.keys.length) {
                    this.keys = Arrays.copyOf(this.keys, this.count * 2);
                    this.sections = Arrays.copyOf(this.sections, this.count * 2);
                }
                this.keys[this.count] = key;
                this.sections[this.count] = found;
                this.count++;
            }
            this.last = found;
            this.lastKey = key;
        }
        return this.last;
    }

    @Override
    public BlockState apply(BlockPos pos) {
        LevelChunkSection section = this.getSection(pos);
        if (section == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return section.getBlockState(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
    }

    @Override
    public void close() {
        try {
            for (int i = 0; i < this.count; i++) {
                this.sections[i].release();
            }
        } finally {
            Arrays.fill(this.sections, 0, this.count, null);
            this.count = 0;
            this.last = null;
            this.level = null;
            this.open = false;
        }
    }
}
