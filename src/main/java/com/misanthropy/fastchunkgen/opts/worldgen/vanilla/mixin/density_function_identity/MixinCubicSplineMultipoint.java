package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_identity;

import net.minecraft.util.CubicSpline;
import net.minecraft.util.ToFloatFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(CubicSpline.Multipoint.class)
public abstract class MixinCubicSplineMultipoint<C, I extends ToFloatFunction<C>> {

    @Shadow @Final private I coordinate;
    @Shadow @Final private float[] locations;
    @Shadow @Final private List<CubicSpline<C, I>> values;
    @Shadow @Final private float[] derivatives;

    @Shadow
    static <C, I extends ToFloatFunction<C>> CubicSpline.Multipoint<C, I> create(I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives) {
        throw new AssertionError();
    }

    /**
     * @author fastchunkgen
     * @reason keep the node when no changes
     */
    @Overwrite
    public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> visitor) {
        I mappedCoordinate = visitor.visit(this.coordinate);
        List<CubicSpline<C, I>> values = this.values;
        int size = values.size();
        Object[] mapped = null;
        for (int i = 0; i < size; i++) {
            CubicSpline<C, I> value = values.get(i);
            CubicSpline<C, I> result = value.mapAll(visitor);
            if (mapped == null && result != value) {
                mapped = values.toArray();
            }
            if (mapped != null) {
                mapped[i] = result;
            }
        }
        if (mapped == null && mappedCoordinate == this.coordinate) {
            return (CubicSpline<C, I>) (Object) this;
        }
        @SuppressWarnings("unchecked")
        List<CubicSpline<C, I>> newValues = mapped == null ? values : (List<CubicSpline<C, I>>) (List<?>) List.of(mapped);
        return create(mappedCoordinate, this.locations, newValues, this.derivatives);
    }

}
