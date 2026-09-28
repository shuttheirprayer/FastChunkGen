package com.misanthropy.fastchunkgen.uncapvd.mixin;

import com.misanthropy.fastchunkgen.uncapvd.ModuleEntryPoint;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChunkMap.class)
public class MixinChunkMap {

    @ModifyArg(method = "setViewDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"), index = 2)
    private int uncapViewDistance(int max) {
        return ModuleEntryPoint.maxViewDistance;
    }

}
