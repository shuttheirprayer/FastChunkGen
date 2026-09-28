package com.misanthropy.fastchunkgen.uncapvd.mixin;

import com.misanthropy.fastchunkgen.uncapvd.ModuleEntryPoint;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DistanceManager.class)
public class MixinDistanceManager {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/DistanceManager$PlayerTicketTracker;<init>(Lnet/minecraft/server/level/DistanceManager;I)V"), index = 1)
    private int uncapPlayerTicketRange(int range) {
        return ModuleEntryPoint.maxViewDistance;
    }

}
