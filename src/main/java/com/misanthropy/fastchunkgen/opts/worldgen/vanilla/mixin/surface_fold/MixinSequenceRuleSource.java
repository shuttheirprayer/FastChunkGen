package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface_fold;

import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.SurfaceFold;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SurfaceRules.SequenceRuleSource.class)
public abstract class MixinSequenceRuleSource {

    @Shadow @Final private List<SurfaceRules.RuleSource> sequence;

    @Unique private volatile SurfaceFold.Plan fcg$plan;

    @Inject(method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$SurfaceRule;", at = @At("HEAD"), cancellable = true)
    private void fcg$skipByY(SurfaceRules.Context context, CallbackInfoReturnable<SurfaceRules.SurfaceRule> cir) {
        final SurfaceFold.Plan known = this.fcg$plan;
        final SurfaceFold.Plan plan = SurfaceFold.planFor(this.sequence, context, known);
        if (plan == null) return;
        if (plan != known) this.fcg$plan = plan;
        final SurfaceRules.SurfaceRule folded = plan.build(this.sequence, context);
        if (folded != null) cir.setReturnValue(folded);
    }
}
