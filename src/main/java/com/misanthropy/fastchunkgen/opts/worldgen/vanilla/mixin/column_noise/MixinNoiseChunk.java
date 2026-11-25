package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.column_noise;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ColumnMarker;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk {

    @Shadow @Final private int cellCountXZ;
    @Shadow @Final private int cellWidth;
    @Shadow @Final private int firstCellX;
    @Shadow @Final private int firstCellZ;

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/RandomState;router()Lnet/minecraft/world/level/levelgen/NoiseRouter;"))
    private NoiseRouter fcg$hoistColumns(NoiseRouter router, @Local(argsOnly = true) RandomState random) {
        return ColumnMarker.hoist(random, router);
    }

    @ModifyReturnValue(method = "wrapNew", at = @At("RETURN"))
    private DensityFunction fcg$wrapColumn(DensityFunction wrapped) {
        return wrapped instanceof ColumnMarker marker
                ? marker.wrap(this.firstCellX * this.cellWidth, this.firstCellZ * this.cellWidth, this.cellCountXZ * this.cellWidth + 1)
                : wrapped;
    }

}
