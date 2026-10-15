package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.CellBounds;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk implements CellBounds.Cells {

    @Unique private static final int COPPER_MIN_Y = 0, COPPER_MAX_Y = 50, IRON_MIN_Y = -60, IRON_MAX_Y = -8;
    @Unique private static final double VEIN_THRESHOLD = 0.399999D;

    @Shadow @Final private List<NoiseChunk.NoiseInterpolator> interpolators;
    @Shadow @Final private Aquifer aquifer;
    @Shadow @Final private NoiseChunk.BlockStateFiller blockStateRule;
    @Shadow @Final private Blender blender;
    @Shadow @Final private int cellWidth;
    @Shadow @Final private int cellHeight;
    @Shadow @Final private int cellNoiseMinY;
    @Shadow @Final private int firstCellZ;
    @Shadow private int cellStartBlockX;
    @Shadow private int cellStartBlockY;
    @Shadow private int cellStartBlockZ;
    @Shadow private int inCellX;
    @Shadow private int inCellY;
    @Shadow private int inCellZ;
    @Shadow private long interpolationCounter;
    @Shadow private long arrayInterpolationCounter;

    @Shadow protected abstract BlockState getInterpolatedState();

    @Unique private NoiseChunk.BlockStateFiller fcg$aquiferFiller;
    @Unique private NoiseChunk.BlockStateFiller fcg$veinFiller;
    @Unique private DensityFunction fcg$veinToggle;
    @Unique private DensityFunction fcg$finalDensity;
    @Unique private boolean fcg$cellSkipChecked;
    @Unique private boolean fcg$canSkipCells;
    @Unique private boolean fcg$veins;
    @Unique private INoiseInterpolator fcg$veinCorners;
    @Unique private CellBounds.Node fcg$bounds;
    @Unique private boolean fcg$sortingCells;
    @Unique private boolean fcg$solidAllowed;
    @Unique private int fcg$cellMode;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;add(Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList$Builder;", ordinal = 0, remap = false))
    private Object fcg$captureAquiferFiller(Object filler) {
        this.fcg$aquiferFiller = (NoiseChunk.BlockStateFiller) filler;
        return filler;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/OreVeinifier;create(Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;)Lnet/minecraft/world/level/levelgen/NoiseChunk$BlockStateFiller;"), index = 0, require = 0)
    private DensityFunction fcg$captureVeinToggle(DensityFunction veinToggle) {
        this.fcg$veinToggle = veinToggle;
        return veinToggle;
    }

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/OreVeinifier;create(Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;)Lnet/minecraft/world/level/levelgen/NoiseChunk$BlockStateFiller;"), require = 0)
    private NoiseChunk.BlockStateFiller fcg$captureVeinFiller(NoiseChunk.BlockStateFiller filler) {
        this.fcg$veinFiller = filler;
        return filler;
    }

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/DensityFunction;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/DensityFunction;"))
    private DensityFunction fcg$captureFinalDensity(DensityFunction finalDensity) {
        this.fcg$finalDensity = finalDensity;
        return finalDensity;
    }

    @Override
    public void fcg$beginCellSorting(boolean solidAllowed) {
        if (!this.fcg$cellSkipChecked) {
            this.fcg$cellSkipChecked = true;
            if (this.fcg$finalDensity instanceof ICacheAllInCell && this.aquifer instanceof CellBounds.HighAir
                    && this.blockStateRule instanceof MaterialRuleList rules) {
                final List<NoiseChunk.BlockStateFiller> list = rules.materialRuleList();
                final boolean aquiferOnly = list.size() == 1 && list.get(0) == this.fcg$aquiferFiller;
                final boolean withVeins = list.size() == 2 && list.get(0) == this.fcg$aquiferFiller && list.get(1) == this.fcg$veinFiller;
                this.fcg$canSkipCells = aquiferOnly || withVeins;
                this.fcg$veins = withVeins;
                this.fcg$veinCorners = this.fcg$veinToggle instanceof NoiseChunk.NoiseInterpolator ? (INoiseInterpolator) this.fcg$veinToggle : null;
            }
            if (this.fcg$canSkipCells) {
                this.fcg$bounds = CellBounds.compile(((ICacheAllInCell) this.fcg$finalDensity).fcg$noiseFiller(),
                        this.cellWidth, this.cellHeight, this.blender == Blender.empty());
            }
        }
        this.fcg$sortingCells = this.fcg$canSkipCells;
        this.fcg$solidAllowed = solidAllowed;
    }

    @Override
    public int fcg$cellMode() {
        return this.fcg$cellMode;
    }

    @Override
    public int fcg$cellWidth() {
        return this.cellWidth;
    }

    @Override
    public int fcg$cellHeight() {
        return this.cellHeight;
    }

    @Override
    public BlockState fcg$interpolatedState() {
        return this.getInterpolatedState();
    }

    @Inject(method = "stopInterpolation", at = @At("HEAD"))
    private void fcg$endCellSorting(CallbackInfo ci) {
        this.fcg$sortingCells = false;
        this.fcg$cellMode = CellBounds.NORMAL;
    }

    @Inject(method = "selectCellYZ", at = @At("HEAD"), cancellable = true)
    private void fcg$skipBoundedCell(int cellY, int cellZ, CallbackInfo ci) {
        this.fcg$cellMode = CellBounds.NORMAL;
        if (!this.fcg$sortingCells || this.fcg$bounds == null) return;
        final int minY = (cellY + this.cellNoiseMinY) * this.cellHeight;
        final int minZ = (this.firstCellZ + cellZ) * this.cellWidth;
        final CellBounds.Node bounds = this.fcg$bounds;
        bounds.eval(cellY, cellZ, this.cellStartBlockX, minY, minZ);
        if (bounds.hi < -CellBounds.MARGIN) {
            if (!((CellBounds.HighAir) this.aquifer).fcg$allHighAir(this.cellStartBlockX, minY, minZ, this.cellWidth, this.cellHeight)) return;
            this.fcg$cellMode = CellBounds.AIR;
        } else if (bounds.lo > CellBounds.MARGIN && this.fcg$solidAllowed && this.fcg$veinsAbsent(cellY, cellZ, minY)) {
            this.fcg$cellMode = CellBounds.SOLID;
        } else {
            return;
        }
        this.cellStartBlockY = minY;
        this.cellStartBlockZ = minZ;
        this.arrayInterpolationCounter += 2;
        this.fcg$skipCellSteps();
        ci.cancel();
    }

    // CacheOnce keys on these counters, so a skipped cell advances them like vanilla's selectCellYZ and per-block updateForZ would
    @Unique
    private void fcg$skipCellSteps() {
        this.interpolationCounter += (long) this.cellWidth * this.cellWidth * this.cellHeight;
        ((CellBounds.HighAir) this.aquifer).fcg$skippedCell(this.fcg$cellMode == CellBounds.SOLID,
                this.cellStartBlockX + this.cellWidth - 1, this.cellStartBlockY, this.cellStartBlockZ + this.cellWidth - 1);
    }

    @Inject(method = "selectCellYZ", at = @At("RETURN"))
    private void fcg$sortFilledCell(int cellY, int cellZ, CallbackInfo ci) {
        if (!this.fcg$sortingCells) return;
        final double[] densities = ((ICacheAllInCell) this.fcg$finalDensity).fcg$values();
        final boolean firstPositive = densities[0] > 0.0D;
        for (double density : densities) {
            if (density > 0.0D != firstPositive) return;
        }
        if (!firstPositive) {
            if (((CellBounds.HighAir) this.aquifer).fcg$allHighAir(this.cellStartBlockX, this.cellStartBlockY, this.cellStartBlockZ, this.cellWidth, this.cellHeight)) {
                this.fcg$cellMode = CellBounds.AIR;
            }
        } else if (this.fcg$solidAllowed && this.fcg$veinsAbsent(cellY, cellZ, this.cellStartBlockY)) {
            this.fcg$cellMode = CellBounds.SOLID;
        }
        if (this.fcg$cellMode != CellBounds.NORMAL) this.fcg$skipCellSteps();
    }

    @Unique
    private boolean fcg$veinsAbsent(int cellY, int cellZ, int minY) {
        if (!this.fcg$veins) return true;
        final int maxY = minY + this.cellHeight - 1;
        final boolean inCopper = maxY >= COPPER_MIN_Y && minY <= COPPER_MAX_Y;
        final boolean inIron = maxY >= IRON_MIN_Y && minY <= IRON_MAX_Y;
        if (!inCopper && !inIron) return true;
        if (this.fcg$veinCorners == null) return false;
        final double[][] slice0 = this.fcg$veinCorners.fcg$slice0();
        final double[][] slice1 = this.fcg$veinCorners.fcg$slice1();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int dz = 0; dz <= 1; dz++) {
            for (int dy = 0; dy <= 1; dy++) {
                final double a = slice0[cellZ + dz][cellY + dy];
                final double b = slice1[cellZ + dz][cellY + dy];
                if (Double.isNaN(a) || Double.isNaN(b)) return false;
                min = Math.min(min, Math.min(a, b));
                max = Math.max(max, Math.max(a, b));
            }
        }
        if (Math.max(Math.abs(min), Math.abs(max)) < VEIN_THRESHOLD) return true;
        if (min > 1.0E-6D) return !inCopper;
        if (max < -1.0E-6D) return !inIron;
        return false;
    }

    /**
     * @author Misanthropy
     * @reason indexed loop
     */
    @Overwrite
    public void updateForY(int blockY, double y) {
        this.inCellY = blockY - this.cellStartBlockY;
        final List<NoiseChunk.NoiseInterpolator> list = this.interpolators;
        for (int i = 0, n = list.size(); i < n; i++) list.get(i).updateForY(y);
    }

    /**
     * @author Misanthropy
     * @reason indexed loop2
     */
    @Overwrite
    public void updateForX(int blockX, double x) {
        this.inCellX = blockX - this.cellStartBlockX;
        final List<NoiseChunk.NoiseInterpolator> list = this.interpolators;
        for (int i = 0, n = list.size(); i < n; i++) list.get(i).updateForX(x);
    }

    /**
     * @author Misanthropy
     * @reason indexed loop3
     */
    @Overwrite
    public void updateForZ(int blockZ, double z) {
        this.inCellZ = blockZ - this.cellStartBlockZ;
        ++this.interpolationCounter;
        final List<NoiseChunk.NoiseInterpolator> list = this.interpolators;
        for (int i = 0, n = list.size(); i < n; i++) list.get(i).updateForZ(z);
    }

}
