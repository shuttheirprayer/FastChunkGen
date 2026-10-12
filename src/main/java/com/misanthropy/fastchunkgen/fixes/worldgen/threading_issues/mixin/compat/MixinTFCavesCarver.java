package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.compat;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "twilightforest.world.components.TFCavesCarver", remap = false)
public abstract class MixinTFCavesCarver {

    @Shadow
    @Final
    @Mutable
    private RandomSource rand;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fastchunkgen$threadSafeRandom(CallbackInfo ci) {
        this.rand = RandomSource.createThreadSafe();
    }
}
