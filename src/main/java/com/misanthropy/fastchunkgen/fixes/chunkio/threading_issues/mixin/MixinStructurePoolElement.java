package com.misanthropy.fastchunkgen.fixes.chunkio.threading_issues.mixin;

import com.misanthropy.fastchunkgen.fixes.chunkio.threading_issues.common.SynchronizedCodec;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructurePoolElement.class)
public class MixinStructurePoolElement {

    @Mutable
    @Shadow @Final public static Codec<StructurePoolElement> CODEC;

    @Dynamic
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onCLInit(CallbackInfo info) {
        CODEC = new SynchronizedCodec<>(CODEC);
    }

}
