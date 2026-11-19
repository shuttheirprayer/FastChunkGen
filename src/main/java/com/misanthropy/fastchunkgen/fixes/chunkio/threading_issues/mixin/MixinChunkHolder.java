package com.misanthropy.fastchunkgen.fixes.chunkio.threading_issues.mixin;

import com.misanthropy.fastchunkgen.base.mixin.access.IThreadedAnvilChunkStorage;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder {

    @Shadow
    @Final
    private ChunkPos pos;

    @Inject(method = "updateFutures", at = @At("HEAD"))
    private void beforeTick(ChunkMap chunkStorage, Executor executor, CallbackInfo ci) {
        IThreadedAnvilChunkStorage map = (IThreadedAnvilChunkStorage) chunkStorage;
        if (map.getChunkHolders().get(this.pos.toLong()) != (Object) this) {
            map.invokeUpdateHolderMap();
        }
    }

}
