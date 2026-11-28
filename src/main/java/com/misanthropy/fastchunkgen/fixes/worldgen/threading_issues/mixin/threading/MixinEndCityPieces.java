package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.threading;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(EndCityPieces.class)
public class MixinEndCityPieces {

    @Unique
    private static final Object fcg$GENERATOR_LOCK = new Object();

    @WrapMethod(method = "startHouseTower")
    private static void fcg$oneCityAtATime(StructureTemplateManager templates, BlockPos pos, Rotation rotation, List<StructurePiece> pieces, RandomSource random, Operation<Void> original) {
        synchronized (fcg$GENERATOR_LOCK) {
            original.call(templates, pos, rotation, pieces, random);
        }
    }
}
