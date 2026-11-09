package com.misanthropy.fastchunkgen.threading.lighting.mixin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ProcessorMailbox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.*;

@Mixin(ChunkMap.class)
public class MixinThreadedAnvilChunkStorage {

    @Shadow
    @Final
    private ServerLevel level;
    private ExecutorService lightThread = null;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ProcessorMailbox;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/thread/ProcessorMailbox;"))
    private ProcessorMailbox<Runnable> onLightExecutorInit(Executor executor, String name) {
        if (!name.equals("light")) return ProcessorMailbox.create(executor, name);
        lightThread = new ThreadPoolExecutor(
                1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setPriority(Thread.NORM_PRIORITY - 1).setDaemon(true).setNameFormat(String.format("%s - Light", level.dimension().location().toDebugFileName())).build()
        );
        return ProcessorMailbox.create(lightThread, name);
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void afterClose(CallbackInfo info) {
        lightThread.shutdown();
    }

}
