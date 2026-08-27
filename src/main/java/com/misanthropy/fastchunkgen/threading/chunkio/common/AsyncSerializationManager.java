package com.misanthropy.fastchunkgen.threading.chunkio.common;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class AsyncSerializationManager {

    public static final boolean DEBUG = Boolean.getBoolean("fastchunkgen.chunkio.debug");

    private static final Logger LOGGER = LoggerFactory.getLogger("FastChunkGen Async Serialization Manager");

    private static final ThreadLocal<ArrayDeque<Scope>> scopeHolder = ThreadLocal.withInitial(ArrayDeque::new);

    private static final LightLayer[] LIGHT_LAYERS = LightLayer.values();

    public static void push(Scope scope) {
        scopeHolder.get().push(scope);
    }

    public static Scope getScope(ChunkPos pos) {
        final Scope scope = scopeHolder.get().peek();
        if (pos == null) return scope;
        if (scope != null) {
            if (scope.pos.equals(pos))
                return scope;
            LOGGER.error("Scope position mismatch! Expected: {} but got {}. This will impact stability. Incompatible mods?", scope.pos, pos, new Throwable());
        }
        return null;
    }

    public static void pop(Scope scope) {
        if (scope != scopeHolder.get().peek()) throw new IllegalArgumentException("Scope mismatch");
        scopeHolder.get().pop();
    }

    public static class Scope {
        public final ChunkPos pos;
        public final Map<LightLayer, LayerLightEventListener> lighting;
        public final Set<BlockPos> blockEntityPositions;
        public final Map<BlockPos, CompoundTag> blockEntityNbts;
        public final boolean lazyBlockEntities;
        private final AtomicBoolean isOpen = new AtomicBoolean(false);

        public Scope(ChunkAccess chunk, ServerLevel world) {
            this.pos = chunk.getPos();
            final Map<LightLayer, LayerLightEventListener> lighting = new EnumMap<>(LightLayer.class);
            for (LightLayer type : LIGHT_LAYERS) {
                lighting.put(type, new CachedLightingView(world.getLightEngine(), this.pos, type));
            }
            this.lighting = lighting;
            this.blockEntityPositions = chunk.getBlockEntitiesPos();

            final boolean isLevelChunk = chunk instanceof LevelChunk;
            final boolean snapshotNow = Config.serializeBlockEntitiesOnMainThread;
            final Map<BlockPos, CompoundTag> nbts = new Object2ObjectOpenHashMap<>(this.blockEntityPositions.size());
            for (BlockPos blockPos : this.blockEntityPositions) {
                CompoundTag serialized = null;
                Throwable failure = null;
                final BlockEntity blockEntity = snapshotNow ? chunk.getBlockEntity(blockPos) : null;
                if (blockEntity != null && !blockEntity.isRemoved()) {
                    try {
                        serialized = blockEntity.saveWithFullMetadata();
                        if (isLevelChunk) serialized.putBoolean("keepPacked", false);
                    } catch (Throwable t) {
                        LOGGER.error("Failed to serialize block entity {} at {} in chunk {}, falling back to its stored data", blockEntity.getType(), blockPos, this.pos, t);
                        serialized = null;
                        failure = t;
                    }
                }
                if (serialized == null) {
                    final CompoundTag stored = chunk.getBlockEntityNbt(blockPos);
                    if (stored == null) {
                        if (failure != null) {
                            throw new RuntimeException("Cannot serialize block entity %s at %s in chunk %s".formatted(blockEntity.getType(), blockPos, this.pos), failure);
                        }
                        continue;
                    }
                    serialized = stored.copy();
                    if (isLevelChunk) serialized.putBoolean("keepPacked", true);
                }
                nbts.put(blockPos, serialized);
            }
            this.blockEntityNbts = nbts;
            this.lazyBlockEntities = !snapshotNow;

            if (DEBUG && this.blockEntityPositions.size() != this.blockEntityNbts.size()) {
                LOGGER.warn("Block entities size mismatch! expected {} but got {}", this.blockEntityPositions.size(), this.blockEntityNbts.size());
            }
        }

        public void open() {
            if (!isOpen.compareAndSet(false, true)) throw new IllegalStateException("Cannot use scope twice");
        }

        private static final class CachedLightingView implements LayerLightEventListener {

            private static final DataLayer EMPTY = new DataLayer();

            private final Map<SectionPos, DataLayer> cachedData = new Object2ObjectOpenHashMap<>();

            CachedLightingView(LevelLightEngine provider, ChunkPos pos, LightLayer type) {
                final LayerLightEventListener listener = provider.getLayerListener(type);
                for (int i = provider.getMinLightSection(); i < provider.getMaxLightSection(); i++) {
                    final SectionPos sectionPos = SectionPos.of(pos, i);
                    DataLayer lighting = listener.getDataLayerData(sectionPos);
                    cachedData.put(sectionPos, lighting != null ? lighting.copy() : null);
                }
            }

            @Override
            public void checkBlock(BlockPos blockPos) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasLightWork() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int runLightUpdates() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void updateSectionStatus(SectionPos pos, boolean notReady) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setLightEnabled(ChunkPos chunkPos, boolean bl) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void propagateLightSources(ChunkPos chunkPos) {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public DataLayer getDataLayerData(SectionPos pos) {
                return cachedData.getOrDefault(pos, EMPTY);
            }

            @Override
            public int getLightValue(BlockPos pos) {
                throw new UnsupportedOperationException();
            }
        }
    }

}
