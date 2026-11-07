package com.misanthropy.fastchunkgen.opts.scheduling.mixin.player_ticket_slots;

import com.misanthropy.fastchunkgen.opts.scheduling.common.Config;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DistanceManager.class)
public class MixinDistanceManager {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkTaskPriorityQueueSorter;<init>(Ljava/util/List;Ljava/util/concurrent/Executor;I)V"), index = 2)
    private int fcg$playerTicketSlots(int vanilla) {
        return Math.max(vanilla, Config.playerTicketSlots());
    }

}
