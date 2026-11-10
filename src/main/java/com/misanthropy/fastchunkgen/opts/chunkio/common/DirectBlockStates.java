package com.misanthropy.fastchunkgen.opts.chunkio.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.*;

public final class DirectBlockStates {

    private static final ThreadLocal<int[]> IDS = ThreadLocal.withInitial(() -> new int[PalettedContainer.Strategy.SECTION_STATES.size()]);

    private DirectBlockStates() {
    }

    public static CompoundTag encode(PalettedContainer<BlockState> container) {
        if (container.getClass() != PalettedContainer.class) return null;
        final int size = PalettedContainer.Strategy.SECTION_STATES.size();
        final int[] ids = IDS.get();
        final List<BlockState> values = new ArrayList<>();
        container.acquire();
        try {
            final PalettedContainer.Data<BlockState> data = container.data;
            final Palette<BlockState> palette = data.palette();
            data.storage().unpack(ids);
            final int[] remap = new int[palette.getSize()];
            Arrays.fill(remap, -1);
            final Map<BlockState, Integer> byValue = new IdentityHashMap<>();
            int lastOld = -1;
            int lastNew = -1;
            for (int i = 0; i < size; i++) {
                final int old = ids[i];
                if (old != lastOld) {
                    lastOld = old;
                    if (old < 0 || old >= remap.length) return null;
                    int mapped = remap[old];
                    if (mapped < 0) {
                        final BlockState value = palette.valueFor(old);
                        final Integer known = byValue.get(value);
                        if (known == null) {
                            mapped = values.size();
                            values.add(value);
                            byValue.put(value, mapped);
                        } else {
                            mapped = known;
                        }
                        remap[old] = mapped;
                    }
                    lastNew = mapped;
                }
                ids[i] = lastNew;
            }
        } finally {
            container.release();
        }
        final ListTag entries = new ListTag();
        for (BlockState value : values) entries.add(NbtUtils.writeBlockState(value));
        final CompoundTag tag = new CompoundTag();
        tag.put("palette", entries);
        final int bits = PalettedContainer.Strategy.SECTION_STATES.calculateBitsForSerialization(Block.BLOCK_STATE_REGISTRY, values.size());
        if (bits != 0) tag.putLongArray("data", new SimpleBitStorage(bits, size, ids).getRaw());
        return tag;
    }
}
