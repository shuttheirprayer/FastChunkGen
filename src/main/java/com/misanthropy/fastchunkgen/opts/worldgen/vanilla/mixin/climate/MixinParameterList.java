package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.climate;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.FlatClimateTree;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ShortcutVerify;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Climate.ParameterList.class)
public abstract class MixinParameterList<T> {

    @Unique
    private static final Object FCG$UNSUPPORTED = new Object();

    @Unique
    private volatile Object fcg$flat;

    @SuppressWarnings("unchecked")
    @Inject(method = "findValueIndex(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    private void fcg$flatSearch(Climate.TargetPoint target, CallbackInfoReturnable<T> cir) {
        Object flat = this.fcg$flat;
        if (flat == null) flat = this.fcg$buildFlat();
        if (flat == FCG$UNSUPPORTED) return;
        final T found = (T) ((FlatClimateTree) flat).find(target);
        if (ShortcutVerify.ENABLED) {
            final Climate.ParameterList<T> self = (Climate.ParameterList<T>) (Object) this;
            final T brute = self.findValueBruteForce(target);
            ShortcutVerify.CLIMATE.record(found == brute || fcg$fitness(self, found, target) == fcg$fitness(self, brute, target), target);
        }
        cir.setReturnValue(found);
    }

    @Unique
    private static <T> long fcg$fitness(Climate.ParameterList<T> list, T value, Climate.TargetPoint target) {
        long best = Long.MAX_VALUE;
        for (Pair<Climate.ParameterPoint, T> pair : list.values()) {
            if (pair.getSecond() == value) {
                final Climate.ParameterPoint p = pair.getFirst();
                final long f = Mth.square(p.temperature().distance(target.temperature())) + Mth.square(p.humidity().distance(target.humidity()))
                        + Mth.square(p.continentalness().distance(target.continentalness())) + Mth.square(p.erosion().distance(target.erosion()))
                        + Mth.square(p.depth().distance(target.depth())) + Mth.square(p.weirdness().distance(target.weirdness()))
                        + Mth.square(p.offset());
                best = Math.min(best, f);
            }
        }
        return best;
    }

    @Unique
    private synchronized Object fcg$buildFlat() {
        if (this.fcg$flat == null) {
            final FlatClimateTree tree = (Object) this.getClass() == Climate.ParameterList.class ? FlatClimateTree.build((Climate.ParameterList<?>) (Object) this) : null;
            this.fcg$flat = tree == null ? FCG$UNSUPPORTED : tree;
        }
        return this.fcg$flat;
    }
}
