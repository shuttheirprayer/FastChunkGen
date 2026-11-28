package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.fill;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.CellBounds;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ShortcutVerify;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk implements CellBounds.Cells {

    @Unique private static final int COPPER_MIN_Y = 0, COPPER_MAX_Y = 50, IRON_MIN_Y = -60, IRON_MAX_Y = -8;
    @Unique private static final double VEIN_THRESHOLD = 0.399999D;
    @Unique private static final BlockState AIR = Blocks.AIR.defaultBlockState();

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
    @Unique private boolean fcg$filling;
    @Unique private boolean fcg$cellsFilled;
    @Unique private int fcg$probedCell;
    @Unique private boolean fcg$solidAllowed;
    @Unique private int fcg$cellMode;
    @Unique private int fcg$verifyMode;
    @Unique private int fcg$cellChecks;

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
        this.fcg$checkCellSkipping();
        this.fcg$filling = true;
        this.fcg$sortingCells = this.fcg$canSkipCells;
        this.fcg$solidAllowed = solidAllowed;
    }

    @Unique
    private void fcg$checkCellSkipping() {
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
            if (this.fcg$canSkipCells && !ShortcutVerify.SKIPPED_CELLS.broken() && !ShortcutVerify.PROBED_CELLS.broken()) {
                this.fcg$bounds = CellBounds.compile(((ICacheAllInCell) this.fcg$finalDensity).fcg$noiseFiller(),
                        this.cellWidth, this.cellHeight, this.blender == Blender.empty());
            }
        }
    }

    @Override
    public int fcg$cellMode() {
        return this.fcg$cellMode;
    }

    @Override
    public boolean fcg$filling() {
        return this.fcg$filling;
    }

    @Override
    public boolean fcg$cellsFilled() {
        return this.fcg$cellsFilled;
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
        return this.blockStateRule.calculate((NoiseChunk) (Object) this);
    }

    @Inject(method = "getInterpolatedState", at = @At("HEAD"), cancellable = true)
    private void fcg$probeBoundedCell(CallbackInfoReturnable<BlockState> cir) {
        if (this.fcg$probedCell == CellBounds.NORMAL) return;
        final BlockState state = this.fcg$probedCell == CellBounds.AIR ? AIR : null;
        if (ShortcutVerify.ENABLED) {
            final BlockState actual = this.blockStateRule.calculate((NoiseChunk) (Object) this);
            ShortcutVerify.PROBED_CELLS.record(state == actual, this.cellStartBlockX + "," + this.cellStartBlockY + "," + this.cellStartBlockZ + " " + state + " " + actual);
        } else if (ShortcutVerify.due(++this.fcg$cellChecks)) {
            final BlockState actual = this.blockStateRule.calculate((NoiseChunk) (Object) this);
            if (state != actual) {
                ShortcutVerify.PROBED_CELLS.broke(this.cellStartBlockX + "," + this.cellStartBlockY + "," + this.cellStartBlockZ);
                cir.setReturnValue(actual);
                return;
            }
        }
        cir.setReturnValue(state);
    }

    @Inject(method = "stopInterpolation", at = @At("HEAD"))
    private void fcg$endCellSorting(CallbackInfo ci) {
        this.fcg$sortingCells = false;
        this.fcg$filling = false;
        this.fcg$cellMode = CellBounds.NORMAL;
    }

    @Inject(method = "selectCellYZ", at = @At("HEAD"), cancellable = true)
    private void fcg$skipBoundedCell(int cellY, int cellZ, CallbackInfo ci) {
        this.fcg$cellMode = CellBounds.NORMAL;
        this.fcg$cellsFilled = this.fcg$filling;
        this.fcg$probedCell = CellBounds.NORMAL;
        if (!this.fcg$filling) {
            final List<NoiseChunk.NoiseInterpolator> list = this.interpolators;
            for (int i = 0, n = list.size(); i < n; i++) ((INoiseInterpolator) list.get(i)).fcg$selectCellYZ(cellY, cellZ);
            this.cellStartBlockY = (cellY + this.cellNoiseMinY) * this.cellHeight;
            this.cellStartBlockZ = (this.firstCellZ + cellZ) * this.cellWidth;
            this.arrayInterpolationCounter += 2;
            this.fcg$probedCell = this.fcg$boundedCell(cellY, cellZ, this.cellStartBlockY, this.cellStartBlockZ, true);
            ci.cancel();
            return;
        }
        if (!this.fcg$sortingCells || this.fcg$bounds == null) return;
        final int minY = (cellY + this.cellNoiseMinY) * this.cellHeight;
        final int minZ = (this.firstCellZ + cellZ) * this.cellWidth;
        this.fcg$cellMode = this.fcg$boundedCell(cellY, cellZ, minY, minZ, this.fcg$solidAllowed);
        if (this.fcg$cellMode == CellBounds.NORMAL) return;
        if (ShortcutVerify.ENABLED || (++this.fcg$cellChecks & 1023) == 0) {
            this.fcg$verifyMode = this.fcg$cellMode;
            this.fcg$cellMode = CellBounds.NORMAL;
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
        if (this.fcg$verifyMode != CellBounds.NORMAL) {
            boolean ok = true;
            for (double density : densities) ok &= density > 0.0D == (this.fcg$verifyMode == CellBounds.SOLID);
            this.fcg$verifyMode = CellBounds.NORMAL;
            if (ShortcutVerify.ENABLED) ShortcutVerify.SKIPPED_CELLS.record(ok, this.cellStartBlockX + "," + this.cellStartBlockY + "," + this.cellStartBlockZ);
            else if (!ok) ShortcutVerify.SKIPPED_CELLS.broke(this.cellStartBlockX + "," + this.cellStartBlockY + "," + this.cellStartBlockZ);
        }
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
    private int fcg$boundedCell(int cellY, int cellZ, int minY, int minZ, boolean solidAllowed) {
        this.fcg$checkCellSkipping();
        final CellBounds.Node bounds = this.fcg$bounds;
        if (bounds == null) return CellBounds.NORMAL;
        bounds.eval(cellY, cellZ, this.cellStartBlockX, minY, minZ);
        if (bounds.hi < -CellBounds.MARGIN) {
            return ((CellBounds.HighAir) this.aquifer).fcg$allHighAir(this.cellStartBlockX, minY, minZ, this.cellWidth, this.cellHeight) ? CellBounds.AIR : CellBounds.NORMAL;
        }
        return bounds.lo > CellBounds.MARGIN && solidAllowed && this.fcg$veinsAbsent(cellY, cellZ, minY) ? CellBounds.SOLID : CellBounds.NORMAL;
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
