package com.misanthropy.fastchunkgen.opts.chunkio.mixin.direct_block_states;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastchunkgen.opts.chunkio.common.DirectBlockStates;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ShortcutVerify;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkSerializer.class)
public abstract class MixinChunkSerializer {

    @Shadow
    @Final
    private static Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", remap = false))
    private static DataResult fcg$directBlockStates(Codec codec, DynamicOps ops, Object value, Operation<DataResult> original) {
        if (codec == BLOCK_STATE_CODEC && ops == NbtOps.INSTANCE && value instanceof PalettedContainer<?> container) {
            final CompoundTag tag = DirectBlockStates.encode((PalettedContainer<BlockState>) container);
            if (tag != null) {
                if (ShortcutVerify.ENABLED || DirectBlockStates.shouldCompare()) {
                    final DataResult vanillaResult = original.call(codec, ops, value);
                    final Object vanilla = vanillaResult.result().orElse(null);
                    final boolean same = vanilla instanceof CompoundTag && DirectBlockStates.sameBlocks(tag, (CompoundTag) vanilla);
                    if (ShortcutVerify.ENABLED) ShortcutVerify.BLOCK_STATES.record(same, vanilla);
                    if (!same) {
                        DirectBlockStates.switchOff();
                        return vanillaResult;
                    }
                }
                return DataResult.success(tag);
            }
        }
        return original.call(codec, ops, value);
    }
}
