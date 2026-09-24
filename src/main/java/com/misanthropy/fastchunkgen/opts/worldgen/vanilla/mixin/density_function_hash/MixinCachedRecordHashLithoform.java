package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.density_function_hash;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = {
        "com.misanthropy.lithoform.density_function_types.Division",
        "com.misanthropy.lithoform.density_function_types.Signum",
        "com.misanthropy.lithoform.density_function_types.Sine",
        "com.misanthropy.lithoform.density_function_types.Sqrt"
}, remap = false)
public abstract class MixinCachedRecordHashLithoform {

    @Unique
    private int fastchunkgen$hash;

    @WrapMethod(method = "hashCode")
    private int fastchunkgen$cachedHash(Operation<Integer> original) {
        int cached = this.fastchunkgen$hash;
        if (cached != 0) {
            return cached;
        }
        int computed = original.call();
        this.fastchunkgen$hash = computed;
        return computed;
    }

}
