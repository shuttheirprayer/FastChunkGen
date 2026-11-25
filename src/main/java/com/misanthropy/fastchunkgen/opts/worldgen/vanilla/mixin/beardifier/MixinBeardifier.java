package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.beardifier;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.CellBounds;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Mixin(Beardifier.class)
public abstract class MixinBeardifier implements CellBounds.BeardBounds {

    @Shadow @Final protected ObjectListIterator<Beardifier.Rigid> pieceIterator;
    @Shadow @Final protected ObjectListIterator<JigsawJunction> junctionIterator;

    @Unique private int fcg$minX = Integer.MAX_VALUE;
    @Unique private int fcg$minY = Integer.MAX_VALUE;
    @Unique private int fcg$minZ = Integer.MAX_VALUE;
    @Unique private int fcg$maxX = Integer.MIN_VALUE;
    @Unique private int fcg$maxY = Integer.MIN_VALUE;
    @Unique private int fcg$maxZ = Integer.MIN_VALUE;
    @Unique private int[] fcg$boxes;
    @Unique private int fcg$boxCount;

    @Unique
    private void fcg$bound() {
        this.fcg$boxes = new int[24];
        if (!CellBounds.onlyVanillaBeardTerms((Beardifier) (Object) this)) {
            this.fcg$include(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            return;
        }
        while (this.pieceIterator.hasNext()) {
            final Beardifier.Rigid rigid = this.pieceIterator.next();
            final TerrainAdjustment adjustment = rigid.terrainAdjustment();
            if (adjustment == TerrainAdjustment.NONE) continue;
            final BoundingBox box = rigid.box();
            if (box.getClass() != BoundingBox.class || (adjustment != TerrainAdjustment.BURY
                    && adjustment != TerrainAdjustment.BEARD_THIN && adjustment != TerrainAdjustment.BEARD_BOX)) {
                this.fcg$include(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
                continue;
            }
            final long groundY = (long) box.minY() + rigid.groundLevelDelta();
            this.fcg$include(box.minX() - 12L, Math.min(groundY, box.minY()) - 12L, box.minZ() - 12L,
                    box.maxX() + 12L, Math.max(groundY, box.maxY()) + 12L, box.maxZ() + 12L);
        }
        this.pieceIterator.back(Integer.MAX_VALUE);
        while (this.junctionIterator.hasNext()) {
            final JigsawJunction junction = this.junctionIterator.next();
            this.fcg$include(junction.getSourceX() - 12L, junction.getSourceGroundY() - 12L, junction.getSourceZ() - 12L,
                    junction.getSourceX() + 12L, junction.getSourceGroundY() + 12L, junction.getSourceZ() + 12L);
        }
        this.junctionIterator.back(Integer.MAX_VALUE);
    }

    @Unique
    private void fcg$include(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        final int at = this.fcg$boxCount++ * 6;
        if (at == this.fcg$boxes.length) this.fcg$boxes = Arrays.copyOf(this.fcg$boxes, at * 2);
        final int[] boxes = this.fcg$boxes;
        boxes[at] = (int) Math.max(Integer.MIN_VALUE, minX);
        boxes[at + 1] = (int) Math.max(Integer.MIN_VALUE, minY);
        boxes[at + 2] = (int) Math.max(Integer.MIN_VALUE, minZ);
        boxes[at + 3] = (int) Math.min(Integer.MAX_VALUE, maxX);
        boxes[at + 4] = (int) Math.min(Integer.MAX_VALUE, maxY);
        boxes[at + 5] = (int) Math.min(Integer.MAX_VALUE, maxZ);
        this.fcg$minX = Math.min(this.fcg$minX, boxes[at]);
        this.fcg$minY = Math.min(this.fcg$minY, boxes[at + 1]);
        this.fcg$minZ = Math.min(this.fcg$minZ, boxes[at + 2]);
        this.fcg$maxX = Math.max(this.fcg$maxX, boxes[at + 3]);
        this.fcg$maxY = Math.max(this.fcg$maxY, boxes[at + 4]);
        this.fcg$maxZ = Math.max(this.fcg$maxZ, boxes[at + 5]);
    }

    @Override
    public boolean fcg$zeroIn(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (this.fcg$boxes == null) this.fcg$bound();
        if (maxX < this.fcg$minX || minX > this.fcg$maxX || maxY < this.fcg$minY || minY > this.fcg$maxY
                || maxZ < this.fcg$minZ || minZ > this.fcg$maxZ) {
            return true;
        }
        final int[] boxes = this.fcg$boxes;
        for (int at = 0, end = this.fcg$boxCount * 6; at < end; at += 6) {
            if (maxX >= boxes[at] && minX <= boxes[at + 3] && maxY >= boxes[at + 1] && minY <= boxes[at + 4]
                    && maxZ >= boxes[at + 2] && minZ <= boxes[at + 5]) {
                return false;
            }
        }
        return true;
    }

    @Redirect(method = "compute", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectListIterator;hasNext()Z", remap = false))
    private boolean fcg$outsideBounds(ObjectListIterator<?> iterator, DensityFunction.FunctionContext context) {
        final int x = context.blockX();
        final int y = context.blockY();
        final int z = context.blockZ();
        if (this.fcg$boxes == null) this.fcg$bound();
        if (x < this.fcg$minX || x > this.fcg$maxX || y < this.fcg$minY || y > this.fcg$maxY || z < this.fcg$minZ || z > this.fcg$maxZ) {
            return false;
        }
        return iterator.hasNext();
    }

}
