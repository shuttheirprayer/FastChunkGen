package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.flat_memo;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.FlatColumnMemos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk implements FlatColumnMemos.Chunk {

    @Unique private int fcg$flatCount;
    @Unique private FlatColumnMemos fcg$flatMemos;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fcg$joinFlatColumnMemos(int cellCountXZ, RandomState random, int firstX, int firstZ, NoiseSettings settings,
                                         DensityFunctions.BeardifierOrMarker beardifier, NoiseGeneratorSettings generatorSettings,
                                         Aquifer.FluidPicker fluidPicker, Blender blender, CallbackInfo ci) {
        if (blender == Blender.empty()) this.fcg$flatMemos = FlatColumnMemos.of(random);
    }

    @Override
    public int fcg$nextFlatIndex() {
        return this.fcg$flatCount++;
    }

    @Override
    public FlatColumnMemos fcg$flatColumnMemos() {
        return this.fcg$flatMemos;
    }
}
