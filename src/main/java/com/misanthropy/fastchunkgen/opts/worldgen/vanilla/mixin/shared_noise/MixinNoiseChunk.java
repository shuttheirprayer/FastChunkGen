package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.shared_noise;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.SharedNoise;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill.INoiseInterpolator;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk {

    @Shadow @Final private List<NoiseChunk.NoiseInterpolator> interpolators;
    @Shadow @Final private List<?> cellCaches;
    @Shadow @Final private DensityFunction initialDensityNoJaggedness;
    @Shadow @Final private DensityFunction.ContextProvider sliceFillingContextProvider;
    @Shadow @Final private int cellWidth;
    @Shadow @Final private int cellHeight;
    @Shadow @Final private int cellCountXZ;
    @Shadow @Final private int firstCellZ;
    @Shadow private int cellStartBlockX;
    @Shadow private int cellStartBlockZ;
    @Shadow private int inCellX;
    @Shadow private int inCellZ;
    @Shadow private long interpolationCounter;
    @Shadow private long arrayInterpolationCounter;

    @Unique private NoiseRouter fcg$router;
    @Unique private SharedNoise fcg$sharedColumns;
    @Unique private SharedNoise fcg$sharedLevels;

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"))
    private NoiseRouter fcg$captureRouter(NoiseRouter router) {
        this.fcg$router = router;
        return router;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fcg$joinSharedNoise(int cellCountXZ, RandomState random, int firstX, int firstZ, NoiseSettings settings,
                                     DensityFunctions.BeardifierOrMarker beardifier, NoiseGeneratorSettings generatorSettings,
                                     Aquifer.FluidPicker fluidPicker, Blender blender, CallbackInfo ci) {
        final NoiseRouter router = this.fcg$router;
        this.fcg$router = null;
        if (blender != Blender.empty() || router == null) return;
        final SharedNoise shared = SharedNoise.of(random, settings.minY(), settings.height(), this.cellWidth, this.cellHeight);
        if (shared == null) return;
        if (shared.levelsShareable(this.initialDensityNoJaggedness)) this.fcg$sharedLevels = shared;
        if (cellCountXZ * this.cellWidth == 16 && (firstX & 15) == 0 && (firstZ & 15) == 0
                && shared.columnsShareable(this.interpolators, router, this.cellCaches)) {
            this.fcg$sharedColumns = shared;
        }
    }

    @Inject(method = "fillSlice", at = @At("HEAD"), cancellable = true)
    private void fcg$fillSliceShared(boolean slice0, int cellX, CallbackInfo ci) {
        final SharedNoise shared = this.fcg$sharedColumns;
        if (shared == null) return;
        final List<NoiseChunk.NoiseInterpolator> interpolators = this.interpolators;
        this.cellStartBlockX = cellX * this.cellWidth;
        this.inCellX = 0;
        final boolean xEdge = (this.cellStartBlockX & 15) == 0;
        for (int i = 0; i < this.cellCountXZ + 1; ++i) {
            this.cellStartBlockZ = (this.firstCellZ + i) * this.cellWidth;
            this.inCellZ = 0;
            ++this.arrayInterpolationCounter;
            final boolean zEdge = (this.cellStartBlockZ & 15) == 0;
            final long key = ColumnPos.asLong(this.cellStartBlockX, this.cellStartBlockZ);
            final long counterSteps = xEdge || zEdge ? shared.takeColumn(key, interpolators, slice0, i) : -1L;
            if (counterSteps >= 0L) {
                this.interpolationCounter += counterSteps;
                continue;
            }
            final long counterBefore = this.interpolationCounter;
            for (int k = 0, n = interpolators.size(); k < n; k++) {
                final NoiseChunk.NoiseInterpolator interpolator = interpolators.get(k);
                final INoiseInterpolator corners = (INoiseInterpolator) interpolator;
                interpolator.fillArray((slice0 ? corners.fcg$slice0() : corners.fcg$slice1())[i], this.sliceFillingContextProvider);
            }
            if (xEdge || zEdge) shared.putColumn(key, (xEdge ? 2 : 1) * (zEdge ? 2 : 1), interpolators, slice0, i, this.interpolationCounter - counterBefore);
        }
        ++this.arrayInterpolationCounter;
        ci.cancel();
    }

    @Inject(method = "computePreliminarySurfaceLevel", at = @At("HEAD"), cancellable = true)
    private void fcg$sharedSurfaceLevel(long column, CallbackInfoReturnable<Integer> cir) {
        final SharedNoise shared = this.fcg$sharedLevels;
        if (shared == null) return;
        final int level = shared.level(column);
        if (level != SharedNoise.NO_LEVEL) cir.setReturnValue(level);
    }

    @Inject(method = "computePreliminarySurfaceLevel", at = @At("RETURN"))
    private void fcg$shareSurfaceLevel(long column, CallbackInfoReturnable<Integer> cir) {
        final SharedNoise shared = this.fcg$sharedLevels;
        if (shared != null) shared.putLevel(column, cir.getReturnValueI());
    }

}
