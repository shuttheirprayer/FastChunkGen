package com.misanthropy.fastchunkgen.rewrites.chunk_serializer.mixin;

import com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common.HeightMapTypeAccessor;
import com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common.NbtWriter;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Heightmap.Types.class)
public class HeightMapTypeMixin implements HeightMapTypeAccessor {
    private byte[] nameBytes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(String enum$name, int enum$ordinal, String name, Heightmap.Usage purpose, Predicate<?> blockPredicate, CallbackInfo ci) {
        this.nameBytes = NbtWriter.getStringBytes(name);
    }

    @Override
    public byte[] getNameBytes() {
        return this.nameBytes;
    }

}

