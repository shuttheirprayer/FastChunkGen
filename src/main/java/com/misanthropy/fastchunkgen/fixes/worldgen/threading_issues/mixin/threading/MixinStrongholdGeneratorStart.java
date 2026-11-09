package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.threading;

import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(StrongholdPieces.StartPiece.class)
public class MixinStrongholdGeneratorStart {

    @Mutable
    @Shadow @Final public List<StructurePiece> pendingChildren;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.pendingChildren = Collections.synchronizedList(pendingChildren);
    }

}
