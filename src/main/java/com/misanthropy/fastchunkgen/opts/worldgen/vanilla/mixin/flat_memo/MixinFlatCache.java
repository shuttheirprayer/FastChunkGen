package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.flat_memo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.FlatColumnMemos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.levelgen.NoiseChunk$FlatCache")
public abstract class MixinFlatCache {

    @Unique private NoiseChunk fcg$outer;
    @Unique private int fcg$index;
    @Unique private FlatColumnMemos.Memo fcg$memo;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fcg$numberFlatCache(NoiseChunk outer, DensityFunction wrapped, boolean fill, CallbackInfo ci) {
        this.fcg$outer = outer;
        this.fcg$index = ((FlatColumnMemos.Chunk) outer).fcg$nextFlatIndex();
    }

    @WrapOperation(method = "compute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D"))
    private double fcg$rememberOutsideColumn(DensityFunction wrapped, DensityFunction.FunctionContext context, Operation<Double> original) {
        FlatColumnMemos.Memo memo = this.fcg$memo;
        if (memo == null) {
            final FlatColumnMemos memos = ((FlatColumnMemos.Chunk) this.fcg$outer).fcg$flatColumnMemos();
            memo = memos == null ? FlatColumnMemos.NONE : memos.memo(this.fcg$index, ((DensityFunctions.MarkerOrMarked) (Object) this).wrapped());
            this.fcg$memo = memo;
        }
        if (context == this.fcg$outer || !memo.active()) return original.call(wrapped, context);
        final int x = context.blockX();
        final int z = context.blockZ();
        final double remembered = memo.find(x, z);
        if (!Double.isNaN(remembered)) {
            return memo.shouldCheck() ? memo.checked(remembered, original.call(wrapped, context), x, z) : remembered;
        }
        final double value = original.call(wrapped, context);
        memo.put(x, z, value);
        return value;
    }
}
