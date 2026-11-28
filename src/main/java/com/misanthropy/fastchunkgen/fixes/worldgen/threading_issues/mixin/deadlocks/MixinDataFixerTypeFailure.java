package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.deadlocks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(Type.class)
public abstract class MixinDataFixerTypeFailure {

    @Shadow(remap = false) @Final private static Map<Object, Object> PENDING_REWRITE_CACHE;

    @WrapOperation(method = "rewrite", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/TypeRewriteRule;rewrite(Lcom/mojang/datafixers/types/Type;)Ljava/util/Optional;"), remap = false)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Optional fcg$releaseOnRuleFailure(TypeRewriteRule rule, Type type, Operation<Optional> original,
                                              @Local Triple key, @Local CompletableFuture pending) {
        try {
            return original.call(rule, type);
        } catch (Throwable t) {
            fcg$release(key, pending, t);
            throw t;
        }
    }

    @WrapOperation(method = "rewrite", at = @At(value = "INVOKE", target = "Ljava/util/Optional;flatMap(Ljava/util/function/Function;)Ljava/util/Optional;"), remap = false)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Optional fcg$releaseOnFlatMapFailure(Optional rewritten, Function function, Operation<Optional> original,
                                                 @Local Triple key, @Local CompletableFuture pending) {
        try {
            return original.call(rewritten, function);
        } catch (Throwable t) {
            fcg$release(key, pending, t);
            throw t;
        }
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void fcg$release(Triple<Type<?>, TypeRewriteRule, PointFreeRule> key, CompletableFuture pending, Throwable cause) {
        PENDING_REWRITE_CACHE.remove(key, pending);
        pending.completeExceptionally(cause);
    }
}
