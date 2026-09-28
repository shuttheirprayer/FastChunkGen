package com.misanthropy.fastchunkgen.uncapvd.mixin;

import com.misanthropy.fastchunkgen.uncapvd.ModuleEntryPoint;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

@Mixin(Options.class)
public class MixinOptions {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;Lnet/minecraft/client/OptionInstance$ValueSet;Ljava/lang/Object;Ljava/util/function/Consumer;)V"), index = 3)
    private OptionInstance.ValueSet<?> uncapRenderDistance(String key, OptionInstance.TooltipSupplier<?> tooltip, OptionInstance.CaptionBasedToString<?> toString, OptionInstance.ValueSet<?> values, Object initial, Consumer<?> onChange) {
        return "options.renderDistance".equals(key) ? new OptionInstance.IntRange(2, ModuleEntryPoint.maxViewDistance) : values;
    }

}
