package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.compat;

import org.spongepowered.asm.mixin.*;

import java.util.LinkedHashMap;
import java.util.function.Function;

@Pseudo
@Mixin(targets = "mcjty.lostcities.worldgen.highway.IntercityHighwayPlanner$BoundedCache", remap = false)
public abstract class MixinLostCitiesBoundedCache {

    @Shadow
    @Final
    private LinkedHashMap<Object, Object> values;

    @Shadow
    abstract Object get(Object key);

    @Shadow
    private void trim() {
    }

    @Overwrite
    Object computeIfAbsent(Object key, Function<Object, Object> factory) {
        final Object cached = this.get(key);
        if (cached != null) return cached;
        final Object computed = factory.apply(key);
        synchronized (this) {
            final Object raced = this.values.putIfAbsent(key, computed);
            if (raced != null) return raced;
            this.trim();
        }
        return computed;
    }
}
