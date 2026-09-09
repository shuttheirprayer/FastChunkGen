package com.misanthropy.fastchunkgen.threading.chunkio.mixin;

import com.misanthropy.fastchunkgen.threading.chunkio.common.AsyncSerializationManager;
import com.misanthropy.fastchunkgen.threading.chunkio.common.ChunkIoMainThreadTaskUtils;
import com.misanthropy.fastchunkgen.threading.chunkio.common.Config;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(ChunkSerializer.class)
public class MixinChunkSerializer {

    @Shadow @Final private static Logger LOGGER;

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;checkConsistencyWithBlocks(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"))
    private static void onPoiStorageInitForPalette(PoiManager instance, SectionPos chunkSectionPos, LevelChunkSection chunkSection) {
        ChunkIoMainThreadTaskUtils.executeMain(() -> instance.checkConsistencyWithBlocks(chunkSectionPos, chunkSection));
    }

    @Redirect(
            method = "read",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", remap = false),
            require = 0,
            remap = false
    )
    private static boolean deferChunkDataLoadEvent(IEventBus bus, Event event) {
        if (Config.forgeChunkDataEventsOnMainThread) {
            ChunkIoMainThreadTaskUtils.executeMain(() -> bus.post(event));
            return false;
        }
        return bus.post(event);
    }

    @Redirect(
            method = "read",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;readCapsFromNBT(Lnet/minecraft/nbt/CompoundTag;)V", remap = false)
    )
    private static void deferCapabilityDeserialization(LevelChunk chunk, CompoundTag caps) {
        if (Config.forgeChunkDataEventsOnMainThread) {
            ChunkIoMainThreadTaskUtils.executeMain(() -> chunk.readCapsFromNBT(caps));
        } else {
            chunk.readCapsFromNBT(caps);
        }
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntitiesPos()Ljava/util/Set;"))
    private static Set<BlockPos> onChunkGetBlockEntityPositions(ChunkAccess chunk) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        return scope != null ? scope.blockEntityPositions : chunk.getBlockEntitiesPos();
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntityNbtForSaving(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag onChunkGetPackedBlockEntityNbt(ChunkAccess chunk, BlockPos pos) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        if (scope == null) return chunk.getBlockEntityNbtForSaving(pos);
        if (scope.lazyBlockEntities) return chunk.getBlockEntityNbtForSaving(pos);
        final CompoundTag nbtCompound = scope.blockEntityNbts.get(pos);
        if (nbtCompound == null && AsyncSerializationManager.DEBUG) LOGGER.warn("Block Entity at {} for block {} doesn't exist", pos, chunk.getBlockState(pos).getBlock());
        return nbtCompound;
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getLayerListener(Lnet/minecraft/world/level/LightLayer;)Lnet/minecraft/world/level/lighting/LayerLightEventListener;"))
    private static LayerLightEventListener onLightingProviderGet(LevelLightEngine lightingProvider, LightLayer lightType) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(null);
        return scope != null ? scope.lighting.get(lightType) : lightingProvider.getLayerListener(lightType);
    }

}
