package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ToFloatFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(CubicSpline.Multipoint.class)
public abstract class MixinCubicSplineMultipoint {

    @Shadow @Final private ToFloatFunction<?> coordinate;
    @Shadow @Final private List<?> values;

    @WrapOperation(method = "mapAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSpline$Multipoint;create(Lnet/minecraft/util/ToFloatFunction;[FLjava/util/List;[F)Lnet/minecraft/util/CubicSpline$Multipoint;"))
    private CubicSpline.Multipoint<?, ?> fastchunkgen$keepUnchanged(ToFloatFunction<?> coordinate, float[] locations, List<?> values, float[] derivatives, Operation<CubicSpline.Multipoint<?, ?>> original) {
        if (coordinate == this.coordinate && values.size() == this.values.size()) {
            boolean same = true;
            for (int i = 0; i < values.size() && same; i++) same = values.get(i) == this.values.get(i);
            if (same) return (CubicSpline.Multipoint<?, ?>) (Object) this;
        }
        return original.call(coordinate, locations, values, derivatives);
    }

}
