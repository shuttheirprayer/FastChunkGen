package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.threading;

import com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.IStrongholdGenerator;
import com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.XPieceDataExtension;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(StrongholdPieces.class)
public class MixinStrongholdGenerator implements IStrongholdGenerator {

    @Unique
    private static final ThreadLocal<List<StrongholdPieces.PieceWeight>> possiblePiecesThreadLocal = ThreadLocal.withInitial(() -> new ArrayList<>());
    @Unique
    private static final ThreadLocal<Integer> totalWeightThreadLocal = ThreadLocal.withInitial(() -> 0);
    @Unique
    private static final ThreadLocal<Class<? extends StrongholdPieces.StrongholdPiece>> activePieceTypeThreadLocal = new ThreadLocal<>();

    @Redirect(method = "resetPieces", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces;currentPieces:Ljava/util/List;", opcode = Opcodes.PUTSTATIC), require = 1)
    private static void redirectSetPossiblePieces(List<StrongholdPieces.PieceWeight> value) {
        possiblePiecesThreadLocal.set(value);
    }

    @Redirect(method = {"resetPieces", "updatePieceWeight", "generatePieceFromSmallDoor"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces;currentPieces:Ljava/util/List;", opcode = Opcodes.GETSTATIC), require = 4)
    private static List<StrongholdPieces.PieceWeight> redirectGetPossiblePieces() {
        return possiblePiecesThreadLocal.get();
    }

    @Redirect(method = {"updatePieceWeight", "generatePieceFromSmallDoor"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$PieceWeight;placeCount:I", opcode = Opcodes.GETFIELD), require = 2)
    private static int redirectGetGeneratedCount(StrongholdPieces.PieceWeight instance) {
        return ((XPieceDataExtension) instance).fcg$getGeneratedCountThreadLocal().get();
    }

    @Redirect(method = {"resetPieces", "generatePieceFromSmallDoor"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$PieceWeight;placeCount:I", opcode = Opcodes.PUTFIELD), require = 2)
    private static void redirectSetGeneratedCount(StrongholdPieces.PieceWeight pieceData, int value) {
        if (value == 0) {
            ((XPieceDataExtension) pieceData).fcg$getGeneratedCountThreadLocal().remove();
        } else {
            ((XPieceDataExtension) pieceData).fcg$getGeneratedCountThreadLocal().set(value);
        }
    }

    @Redirect(method = "updatePieceWeight", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces;totalWeight:I", opcode = Opcodes.PUTSTATIC))
    private static void redirectSetTotalWeight(int value) {
        totalWeightThreadLocal.set(value);
    }

    @Redirect(method = {"generatePieceFromSmallDoor", "updatePieceWeight"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces;totalWeight:I", opcode = Opcodes.GETSTATIC))
    private static int redirectGetTotalWeight() {
        return totalWeightThreadLocal.get();
    }

    @Redirect(method = "generatePieceFromSmallDoor", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces;imposedPiece:Ljava/lang/Class;", opcode = Opcodes.PUTSTATIC))
    private static void redirectSetActivePieceType(Class<? extends StrongholdPieces.StrongholdPiece> value) {
        activePieceTypeThreadLocal.set(value);
    }

    @Redirect(method = "generatePieceFromSmallDoor", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces;imposedPiece:Ljava/lang/Class;", opcode = Opcodes.GETSTATIC))
    private static Class<? extends StrongholdPieces.StrongholdPiece> redirectGetActivePieceType() {
        return activePieceTypeThreadLocal.get();
    }

    @Override
    public ThreadLocal<Class<? extends StrongholdPieces.StrongholdPiece>> getActivePieceTypeThreadLocal() {
        return activePieceTypeThreadLocal;
    }
}
