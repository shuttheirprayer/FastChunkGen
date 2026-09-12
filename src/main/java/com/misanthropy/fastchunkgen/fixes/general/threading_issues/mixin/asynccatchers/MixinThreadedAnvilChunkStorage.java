package com.misanthropy.fastchunkgen.fixes.general.threading_issues.mixin.asynccatchers;

import com.misanthropy.fastchunkgen.base.common.util.Log;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow protected abstract void addEntity(Entity entity);

    @Shadow protected abstract void removeEntity(Entity entity);

    @Unique
    private static final long REPORT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30L);
    @Unique
    private static final AtomicLong LAST_REPORT = new AtomicLong(System.nanoTime() - REPORT_INTERVAL_NANOS - 1L);

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void preventAsyncEntityLoad(Entity entity, CallbackInfo ci) {
        if (this.mainThreadExecutor.isSameThread()) return;
        fcg$reportOffThread("ChunkMap.addEntity", entity);
        ci.cancel();
        this.mainThreadExecutor.execute(() -> {
            if (!entity.isRemoved()) this.addEntity(entity);
        });
    }

    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    private void preventAsyncEntityUnload(Entity entity, CallbackInfo ci) {
        if (this.mainThreadExecutor.isSameThread()) return;
        fcg$reportOffThread("ChunkMap.removeEntity", entity);
        ci.cancel();
        this.mainThreadExecutor.execute(() -> this.removeEntity(entity));
    }

    @Unique
    private static void fcg$reportOffThread(String method, Entity entity) {
        final long now = System.nanoTime();
        final long last = LAST_REPORT.get();
        if (now - last < REPORT_INTERVAL_NANOS || !LAST_REPORT.compareAndSet(last, now)) return;
        Log.ASYNC_CATCHER.warn(
                "{} was called for {} from thread '{}' instead of the server thread. This is a bug in the mod in the stacktrace below, not in FastChunkGen. The call has been moved to the server thread so the game can keep running.",
                method, entity, Thread.currentThread().getName(), new Throwable("Off-thread call site"));
    }

}
