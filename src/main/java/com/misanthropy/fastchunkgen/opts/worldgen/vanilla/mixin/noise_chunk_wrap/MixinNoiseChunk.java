package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.noise_chunk_wrap;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;
import java.util.function.Function;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk implements DensityFunction.Visitor {

    @Shadow
    @Final
    private Map<DensityFunction, DensityFunction> wrapped;

    @Shadow
    protected abstract DensityFunction wrapNew(DensityFunction function);

    @Unique
    private Function<DensityFunction, DensityFunction> fastchunkgen$wrapNew;

    /**
     * @author fastchunkgen
     * @reason only marker nodes need the shared wrapper map!! Everything else will go to straight to wrapNew without a lambda per node
     */
    @Overwrite
    protected DensityFunction wrap(DensityFunction function) {
        if (function instanceof DensityFunctions.MarkerOrMarked) {
            Function<DensityFunction, DensityFunction> wrapNew = this.fastchunkgen$wrapNew;
            if (wrapNew == null) {
                wrapNew = f -> this.wrapNew(f);
                this.fastchunkgen$wrapNew = wrapNew;
            }
            return this.wrapped.computeIfAbsent(function, wrapNew);
        }
        return this.wrapNew(function);
    }

    @Override
    public DensityFunction apply(DensityFunction function) {
        return this.wrap(function);
    }

    @ModifyArg(
            method = {"<init>", "cachedClimateSampler"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunction;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/DensityFunction;"
            )
    )
    private DensityFunction.Visitor fastchunkgen$visitAsSelf(DensityFunction.Visitor visitor) {
        return this;
    }

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
            )
    )
    private DensityFunction.Visitor fastchunkgen$visitRouterAsSelf(DensityFunction.Visitor visitor) {
        return this;
    }

}
