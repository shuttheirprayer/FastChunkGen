package com.misanthropy.fastchunkgen.opts.allocs.mixin;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

@Mixin(CompoundTag.class)
public class MixinNbtCompound {

    @Shadow @Final private Map<String, Tag> tags;

    /**
     * @author ishland
     * @reason copy using fastutil map
     */
    @Overwrite
    public CompoundTag copy() {
        final Map<String, Tag> map = new Object2ObjectOpenHashMap<>(this.tags.size());
        this.tags.forEach((key, value) -> map.put(key, value.copy()));
        return new CompoundTag(map);
    }

    /**
     * copy using fastutil map
     */
    @ModifyArg(method = "<init>()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;<init>(Ljava/util/Map;)V"), index = 0)
    private static Map<String, Tag> modifyMap(Map<String, Tag> map) {
        return new Object2ObjectOpenHashMap<>();
    }

    @Redirect(method = "<init>()V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false))
    private static <K, V> HashMap<K, V> redirectNewHashMap() {
        return null; // avoid double map creation
    }

}
