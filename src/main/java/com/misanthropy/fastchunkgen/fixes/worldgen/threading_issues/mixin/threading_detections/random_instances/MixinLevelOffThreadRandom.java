package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.threading_detections.random_instances;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevelOffThreadRandom {

    @Shadow @Final private Thread thread;

    private static final ThreadLocal<RandomSource> fcg$offThreadRandom =
            ThreadLocal.withInitial(() -> new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed()));

    @Inject(method = "getRandom", at = @At("HEAD"), cancellable = true, require = 0)
    private void fcg$safeOffThreadRandom(CallbackInfoReturnable<RandomSource> cir) {
        final Thread owner = this.thread;
        if (owner != null && Thread.currentThread() != owner) {
            cir.setReturnValue(fcg$offThreadRandom.get());
        }
    }

}
