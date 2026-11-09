package com.misanthropy.fastchunkgen.opts.scheduling.mixin.fix_unload;

import com.misanthropy.fastchunkgen.base.common.structs.LongHashSet;
import com.misanthropy.fastchunkgen.base.common.util.ShouldKeepTickingUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow protected abstract void processUnloads(BooleanSupplier shouldKeepTicking);

    @Mutable
    @Shadow @Final private LongSet toDrop;

    @ModifyArg(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;tick(Ljava/util/function/BooleanSupplier;)V"))
    private BooleanSupplier redirectTickPointOfInterestStorageTick(BooleanSupplier shouldKeepTicking) {
        return ShouldKeepTickingUtils.minimumTicks(shouldKeepTicking, 32);
    }

    @ModifyArg(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;processUnloads(Ljava/util/function/BooleanSupplier;)V"))
    private BooleanSupplier redirectTickUnloadChunks(BooleanSupplier shouldKeepTicking) {
        return () -> true;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.toDrop = new LongHashSet();
    }

}
