package com.misanthropy.fastchunkgen.threading.worldgen.mixin;

import com.ibm.asyncutil.locks.AsyncLock;
import com.misanthropy.fastchunkgen.threading.worldgen.common.IWorldGenLockable;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class MixinServerWorld implements IWorldGenLockable {

    private volatile AsyncLock worldGenSingleThreadedLock = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initWorldGenSingleThreadedLock(CallbackInfo ci) {
        worldGenSingleThreadedLock = AsyncLock.createFair();
    }

    @Override
    public AsyncLock getWorldGenSingleThreadedLock() {
        return worldGenSingleThreadedLock;
    }
}
