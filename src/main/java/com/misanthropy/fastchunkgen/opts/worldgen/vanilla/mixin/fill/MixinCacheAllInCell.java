package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.CellBounds;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.level.levelgen.NoiseChunk$CacheAllInCell")
public abstract class MixinCacheAllInCell {

    @ModifyExpressionValue(method = "compute", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/NoiseChunk;inCellX:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int fcg$skipUnfilledCell(int inCellX, @Local(argsOnly = true) DensityFunction.FunctionContext context) {
        return ((CellBounds.Cells) context).fcg$cellsFilled() ? inCellX : -1;
    }

}
