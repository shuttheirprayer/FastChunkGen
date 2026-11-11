package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.compat;

import org.spongepowered.asm.mixin.*;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Pseudo
@Mixin(targets = "mcjty.lostcities.worldgen.highway.IntercityHighwayPlanner$BoundedCache", remap = false)
public abstract class MixinLostCitiesBoundedCache {

    @Shadow
    @Final
    private LinkedHashMap<Object, Object> values;

    @Unique
    private final ConcurrentHashMap<Object, CompletableFuture<Object>> fastchunkgen$inFlight = new ConcurrentHashMap<>();

    @Shadow
    abstract Object get(Object key);

    @Shadow
    private void trim() {
    }

    @Overwrite
    Object computeIfAbsent(Object key, Function<Object, Object> factory) {
        final Object cached = this.get(key);
        if (cached != null) return cached;
        final CompletableFuture<Object> claim = new CompletableFuture<>();
        final CompletableFuture<Object> running = this.fastchunkgen$inFlight.putIfAbsent(key, claim);
        if (running != null) return running.join();
        try {
            Object value;
            synchronized (this) {
                value = this.values.get(key);
            }
            if (value == null) {
                value = factory.apply(key);
                synchronized (this) {
                    this.values.put(key, value);
                    this.trim();
                }
            }
            claim.complete(value);
            return value;
        } catch (Throwable t) {
            claim.completeExceptionally(t);
            throw t;
        } finally {
            this.fastchunkgen$inFlight.remove(key, claim);
        }
    }
}
