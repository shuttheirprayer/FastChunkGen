package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "org.moddingx.libx.util.lazy.LazyValue", remap = false)
public abstract class MixinLibXLazyValue {

    @Unique
    private volatile boolean fastchunkgen$resolved;

    @WrapMethod(method = "get")
    private Object fastchunkgen$resolveOnce(Operation<Object> original) {
        if (!this.fastchunkgen$resolved) {
            synchronized (this) {
                if (!this.fastchunkgen$resolved) {
                    try {
                        return original.call();
                    } finally {
                        this.fastchunkgen$resolved = true;
                    }
                }
            }
        }
        return original.call();
    }
}
