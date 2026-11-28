package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.jigsaw_rewind;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.JigsawRewind;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public abstract class MixinPlacer {

    @Shadow @Final private StructureTemplateManager structureTemplateManager;
    @Shadow @Final private List<?> pieces;

    @Unique private Iterator<?> fcg$candidates;
    @Unique private ReferenceOpenHashSet<Object> fcg$tried;
    @Unique private Reference2ObjectOpenHashMap<Object, int[]> fcg$bounds;
    @Unique private Object fcg$repeat;
    @Unique private Object fcg$unverified;
    @Unique private int fcg$piecesBefore;

    @WrapOperation(method = "tryPlacingChildren", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;", ordinal = 1), require = 0)
    private Object fcg$noteRepeat(Iterator<?> iterator, Operation<Object> original) {
        final Object element = original.call(iterator);
        if (this.fcg$unverified != null) this.fcg$verifyFailure();
        if (iterator != this.fcg$candidates) {
            this.fcg$candidates = iterator;
            if (this.fcg$tried != null) this.fcg$tried.clear();
        }
        this.fcg$repeat = null;
        if (element != null && JigsawRewind.supported(element)) {
            if (this.fcg$tried == null) this.fcg$tried = new ReferenceOpenHashSet<>();
            if (!this.fcg$tried.add(element)) this.fcg$repeat = element;
        }
        return element;
    }

    @WrapOperation(method = "tryPlacingChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Rotation;getShuffled(Lnet/minecraft/util/RandomSource;)Ljava/util/List;", ordinal = 0), require = 0)
    private List<Rotation> fcg$rewindRepeat(RandomSource random, Operation<List<Rotation>> original) {
        final Object repeat = this.fcg$repeat;
        if (repeat == null) return original.call(random);
        this.fcg$repeat = null;
        if (!JigsawRewind.active()) return original.call(random);
        final StructurePoolElement element = (StructurePoolElement) repeat;
        try {
            if (this.fcg$bounds == null) this.fcg$bounds = new Reference2ObjectOpenHashMap<>();
            int[] bounds = this.fcg$bounds.get(repeat);
            final boolean first = bounds == null;
            if (first) {
                bounds = JigsawRewind.record(element, this.structureTemplateManager);
                this.fcg$bounds.put(repeat, bounds);
            }
            if (JigsawRewind.unusable(bounds)) return original.call(random);
            if (JigsawRewind.shouldCompare(first)) {
                if (!JigsawRewind.sameConsumption(element, this.structureTemplateManager, bounds)) {
                    JigsawRewind.switchOff("a replayed repeat leaves the random elsewhere than the game does");
                    return original.call(random);
                }
                if (this.fcg$unverified == null) {
                    this.fcg$unverified = repeat;
                    this.fcg$piecesBefore = this.pieces.size();
                    return original.call(random);
                }
            }
            JigsawRewind.replay(random, bounds);
            return Collections.emptyList();
        } catch (RuntimeException | LinkageError e) {
            JigsawRewind.switchOff(e.toString());
            return original.call(random);
        }
    }

    @Unique
    private void fcg$verifyFailure() {
        final Object element = this.fcg$unverified;
        this.fcg$unverified = null;
        if (this.pieces.size() > this.fcg$piecesBefore && this.pieces.get(this.fcg$piecesBefore) instanceof PoolElementStructurePiece piece && piece.getElement() == element) {
            JigsawRewind.switchOff("a repeat of an element that had already failed placed a piece");
        }
    }
}
