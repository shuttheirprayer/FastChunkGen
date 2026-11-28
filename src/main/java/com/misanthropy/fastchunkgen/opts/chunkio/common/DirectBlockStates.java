package com.misanthropy.fastchunkgen.opts.chunkio.common;

import com.misanthropy.fastchunkgen.base.common.util.Log;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DirectBlockStates {

    private static final ThreadLocal<int[]> IDS = ThreadLocal.withInitial(() -> new int[PalettedContainer.Strategy.SECTION_STATES.size()]);

    private static final Map<BlockState, CompoundTag> STATE_TAGS = new ConcurrentHashMap<>();

    private static final AtomicLong ENCODED = new AtomicLong();
    private static volatile boolean off;

    private DirectBlockStates() {
    }

    public static boolean shouldCompare() {
        final long n = ENCODED.incrementAndGet();
        return n <= 200L || (n & 1023L) == 0L;
    }

    public static void switchOff() {
        if (!off) {
            off = true;
            Log.MAIN.error("Direct block state encoding switched off for this run: a section's encoded block palette differed from the vanilla codec's");
        }
    }

    private static CompoundTag stateTag(BlockState state) {
        final CompoundTag known = STATE_TAGS.get(state);
        if (known != null) return known;
        final CompoundTag made = frozen(NbtUtils.writeBlockState(state));
        final CompoundTag raced = STATE_TAGS.putIfAbsent(state, made);
        return raced == null ? made : raced;
    }

    private static CompoundTag frozen(CompoundTag tag) {
        final Map<String, Tag> entries = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            final Tag value = tag.get(key);
            entries.put(key, value instanceof CompoundTag inner ? frozen(inner) : value);
        }
        return new CompoundTag(Collections.unmodifiableMap(entries));
    }

    public static boolean sameBlocks(CompoundTag a, CompoundTag b) {
        if (a.equals(b)) return true;
        final ListTag first = a.getList("palette", 10), second = b.getList("palette", 10);
        for (int i = 0; i < PalettedContainer.Strategy.SECTION_STATES.size(); i++) {
            if (!first.get(id(a, first.size(), i)).equals(second.get(id(b, second.size(), i)))) return false;
        }
        return true;
    }

    private static int id(CompoundTag tag, int entries, int index) {
        final int bits = PalettedContainer.Strategy.SECTION_STATES.calculateBitsForSerialization(Block.BLOCK_STATE_REGISTRY, entries);
        return bits == 0 ? 0 : new SimpleBitStorage(bits, PalettedContainer.Strategy.SECTION_STATES.size(), tag.getLongArray("data")).get(index);
    }

    public static CompoundTag encode(PalettedContainer<BlockState> container) {
        if (off || container.getClass() != PalettedContainer.class) return null;
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
        for (BlockState value : values) entries.add(stateTag(value));
        final CompoundTag tag = new CompoundTag();
        tag.put("palette", entries);
        final int bits = PalettedContainer.Strategy.SECTION_STATES.calculateBitsForSerialization(Block.BLOCK_STATE_REGISTRY, values.size());
        if (bits != 0) tag.putLongArray("data", new SimpleBitStorage(bits, size, ids).getRaw());
        return tag;
    }
}
