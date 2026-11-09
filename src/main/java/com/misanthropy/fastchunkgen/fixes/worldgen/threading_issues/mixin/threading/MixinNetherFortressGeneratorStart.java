package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.threading;

import com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.XPieceDataExtension;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(NetherFortressPieces.StartPiece.class)
public class MixinNetherFortressGeneratorStart {

    @Shadow public List<NetherFortressPieces.PieceWeight> availableBridgePieces;
    @Shadow public List<NetherFortressPieces.PieceWeight> availableCastlePieces;

    @Redirect(method = "<init>(Lnet/minecraft/util/RandomSource;II)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/NetherFortressPieces$PieceWeight;placeCount:I", opcode = Opcodes.PUTFIELD), require = 2)
    private void redirectSetPieceDataGeneratedCount(NetherFortressPieces.PieceWeight pieceData, int value) {
        if (value == 0) {
            ((XPieceDataExtension) pieceData).fcg$getGeneratedCountThreadLocal().remove();
        } else {
            ((XPieceDataExtension) pieceData).fcg$getGeneratedCountThreadLocal().set(value);
        }
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.availableBridgePieces = Collections.synchronizedList(this.availableBridgePieces);
        this.availableCastlePieces = Collections.synchronizedList(this.availableCastlePieces);
    }
}
