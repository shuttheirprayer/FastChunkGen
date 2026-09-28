package com.misanthropy.fastchunkgen.uncapvd.mixin;

import net.minecraft.server.level.ChunkTaskPriorityQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkTaskPriorityQueue.class)
public class MixinChunkTaskPriorityQueue {

    @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true)
    private int clampLevel(int level) {
        return Math.min(level, ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
    }

    @ModifyVariable(method = "resortChunkTasks", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int clampFromLevel(int level) {
        return Math.min(level, ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
    }

    @ModifyVariable(method = "resortChunkTasks", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int clampToLevel(int level) {
        return Math.min(level, ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
    }

}
